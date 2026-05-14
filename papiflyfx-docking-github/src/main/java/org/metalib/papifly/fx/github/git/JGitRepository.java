package org.metalib.papifly.fx.github.git;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.TagOpt;
import org.metalib.papifly.fx.github.model.BranchRef;
import org.metalib.papifly.fx.github.model.CommitInfo;
import org.metalib.papifly.fx.github.model.CurrentRefState;
import org.metalib.papifly.fx.github.model.GitRefKind;
import org.metalib.papifly.fx.github.model.RepoStatus;
import org.metalib.papifly.fx.github.model.RollbackMode;
import org.metalib.papifly.fx.github.model.TagRef;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class JGitRepository implements GitRepository {

    private final Repository repository;
    private final Git git;
    private final Supplier<CredentialsProvider> credentialsProviderSupplier;

    public JGitRepository(Path localClonePath, CredentialsProvider credentialsProvider) {
        this(localClonePath, () -> credentialsProvider);
    }

    public JGitRepository(Path localClonePath, Supplier<CredentialsProvider> credentialsProviderSupplier) {
        this.credentialsProviderSupplier = credentialsProviderSupplier;
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder()
                .setWorkTree(localClonePath.toFile())
                .readEnvironment()
                .findGitDir(localClonePath.toFile())
                .setMustExist(true);
            this.repository = builder.build();
            this.git = new Git(repository);
        } catch (IOException ex) {
            throw new GitOperationException("Cannot open repository at " + localClonePath, ex);
        }
    }

    @Override
    public RepoStatus loadStatus() {
        try {
            String currentBranch = currentBranchName();
            String defaultBranch = detectDefaultBranch();
            boolean detached = isDetachedHead();
            Status status = git.status().call();

            int ahead = 0;
            int behind = 0;
            if (!detached) {
                BranchTrackingStatus trackingStatus = BranchTrackingStatus.of(repository, currentBranch);
                if (trackingStatus != null) {
                    ahead = trackingStatus.getAheadCount();
                    behind = trackingStatus.getBehindCount();
                }
            }

            return new RepoStatus(
                currentBranch,
                defaultBranch,
                detached,
                ahead,
                behind,
                copySet(status.getAdded()),
                copySet(status.getChanged()),
                copySet(status.getRemoved()),
                copySet(status.getMissing()),
                copySet(status.getModified()),
                copySet(status.getUntracked())
            );
        } catch (GitAPIException | IOException ex) {
            throw new GitOperationException("Failed to load repository status", ex);
        }
    }

    @Override
    public List<BranchRef> listBranches() {
        try {
            List<Ref> refs = git.branchList()
                .setListMode(ListBranchCommand.ListMode.ALL)
                .call();
            String current = currentBranchName();
            List<BranchRef> result = new ArrayList<>();
            for (Ref ref : refs) {
                String fullName = ref.getName();
                if (fullName == null || fullName.endsWith("/HEAD")) {
                    continue;
                }
                boolean local = fullName.startsWith(Constants.R_HEADS);
                boolean remote = fullName.startsWith(Constants.R_REMOTES);
                String remoteName = remote ? remoteName(fullName) : "";
                String branchName = local ? Repository.shortenRefName(fullName) : remoteBranchName(fullName);
                boolean currentBranch = local && Objects.equals(branchName, current);
                String trackingTarget = local ? trackingLabel(branchName) : "";
                result.add(new BranchRef(branchName, fullName, local, remote, currentBranch, remoteName, trackingTarget));
            }
            result.sort(BranchRef::compareTo);
            return List.copyOf(result);
        } catch (GitAPIException | IOException ex) {
            throw new GitOperationException("Failed to list branches", ex);
        }
    }

    @Override
    public List<TagRef> listTags() {
        try {
            ObjectId headId = repository.resolve(Constants.HEAD);
            List<TagRef> result = new ArrayList<>();
            for (Ref ref : git.tagList().call()) {
                String fullName = ref.getName();
                if (fullName == null) {
                    continue;
                }
                String shortName = Repository.shortenRefName(fullName);
                boolean current = headId != null && headId.equals(resolveObjectId(ref));
                result.add(new TagRef(shortName, fullName, current));
            }
            result.sort(TagRef::compareTo);
            return List.copyOf(result);
        } catch (GitAPIException ex) {
            throw new GitOperationException("Failed to list tags", ex);
        } catch (IOException ex) {
            throw new GitOperationException("Failed to resolve current tag", ex);
        }
    }

    @Override
    public CurrentRefState loadCurrentRef() {
        try {
            String defaultBranch = detectDefaultBranch();
            if (!isDetachedHead()) {
                String branchName = currentBranchName();
                return new CurrentRefState(
                    branchName,
                    Constants.R_HEADS + branchName,
                    GitRefKind.LOCAL_BRANCH,
                    trackingLabel(branchName),
                    CurrentRefState.StatusDotState.CLEAN,
                    branchName.equals(defaultBranch),
                    false,
                    false
                );
            }
            ObjectId headId = repository.resolve(Constants.HEAD);
            TagRef tag = resolveCurrentTag(headId);
            if (tag != null) {
                return new CurrentRefState(
                    tag.name(),
                    tag.fullName(),
                    GitRefKind.TAG,
                    "",
                    CurrentRefState.StatusDotState.CLEAN,
                    false,
                    true,
                    false
                );
            }
            String shortHash = currentBranchName();
            return new CurrentRefState(
                shortHash,
                headId == null ? shortHash : headId.getName(),
                GitRefKind.DETACHED_COMMIT,
                "",
                CurrentRefState.StatusDotState.CLEAN,
                false,
                true,
                false
            );
        } catch (GitAPIException | IOException ex) {
            throw new GitOperationException("Failed to resolve current ref", ex);
        }
    }

    @Override
    public void checkout(String branchName, boolean force) {
        validateBranchName(branchName, "branchName");
        try {
            git.checkout().setName(shortenLocalBranchName(branchName)).call();
        } catch (CheckoutConflictException conflictException) {
            if (!force) {
                throw new GitOperationException("Checkout blocked by local changes", conflictException);
            }
            forceCheckout(shortenLocalBranchName(branchName));
        } catch (RefNotFoundException notFoundException) {
            checkoutRemoteBranch(branchName, force);
        } catch (GitAPIException ex) {
            throw new GitOperationException("Failed to checkout branch " + branchName, ex);
        }
    }

    @Override
    public void checkoutRef(String refName, GitRefKind kind, boolean force) {
        validateBranchName(refName, "refName");
        switch (kind) {
            case LOCAL_BRANCH -> checkout(refName, force);
            case REMOTE_BRANCH -> checkoutRemoteBranch(refName, force);
            case TAG, DETACHED_COMMIT -> checkoutDetachedRef(refName, force);
        }
    }

    @Override
    public void createAndCheckout(String branchName, String startPoint) {
        validateBranchName(branchName, "branchName");
        try {
            CheckoutCommand command = git.checkout()
                .setCreateBranch(true)
                .setName(branchName);
            if (startPoint != null && !startPoint.isBlank()) {
                command.setStartPoint(startPoint);
            }
            command.call();
        } catch (GitAPIException ex) {
            throw new GitOperationException("Failed to create and checkout branch " + branchName, ex);
        }
    }

    @Override
    public CommitInfo commitAll(String message) {
        if (message == null || message.isBlank()) {
            throw new GitOperationException("Commit message is required");
        }
        try {
            git.add().addFilepattern(".").call();
            git.add().addFilepattern(".").setUpdate(true).call();
            RevCommit commit = git.commit().setMessage(message.trim()).call();
            return toCommitInfo(commit);
        } catch (GitAPIException ex) {
            throw new GitOperationException("Failed to create commit", ex);
        }
    }

    @Override
    public CommitInfo getHeadCommit() {
        try {
            ObjectId head = repository.resolve(Constants.HEAD);
            if (head == null) {
                throw new GitOperationException("Repository does not have any commits");
            }
            try (RevWalk walk = new RevWalk(repository)) {
                RevCommit commit = walk.parseCommit(head);
                return toCommitInfo(commit);
            }
        } catch (IOException ex) {
            throw new GitOperationException("Failed to resolve HEAD commit", ex);
        }
    }

    @Override
    public void rollback(RollbackMode mode) {
        try {
            switch (mode) {
                case RESET_SOFT -> git.reset().setMode(ResetCommand.ResetType.SOFT).setRef("HEAD~1").call();
                case RESET_HARD -> git.reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD~1").call();
                case REVERT -> revertHeadCommit();
            }
        } catch (GitAPIException | IOException ex) {
            throw new GitOperationException("Failed to rollback commit with mode " + mode, ex);
        }
    }

    @Override
    public void push(String remoteName) {
        String resolvedRemote = (remoteName == null || remoteName.isBlank()) ? "origin" : remoteName;
        try {
            String branch = currentBranchName();
            String sourceRef = Constants.R_HEADS + branch;
            String targetRef = Constants.R_HEADS + branch;
            Iterable<PushResult> results = git.push()
                .setRemote(resolvedRemote)
                .setRefSpecs(new RefSpec(sourceRef + ":" + targetRef))
                .setCredentialsProvider(credentialsProviderSupplier.get())
                .call();
            validatePushResults(results);
        } catch (TransportException ex) {
            String message = ex.getMessage() == null ? "Push failed" : ex.getMessage();
            throw new GitOperationException("Push failed: " + message, ex);
        } catch (GitAPIException | IOException ex) {
            throw new GitOperationException("Push failed", ex);
        }
    }

    @Override
    public void update() {
        try {
            git.fetch()
                .setRemote("origin")
                .setRemoveDeletedRefs(true)
                .setTagOpt(TagOpt.FETCH_TAGS)
                .setCredentialsProvider(credentialsProviderSupplier.get())
                .call();
        } catch (GitAPIException ex) {
            String message = ex.getMessage() == null ? "Update failed" : ex.getMessage();
            throw new GitOperationException("Update failed: " + message, ex);
        }
    }

    @Override
    public boolean isHeadPushed() {
        try {
            if (isDetachedHead()) {
                return false;
            }
            String branchName = currentBranchName();
            BranchTrackingStatus trackingStatus = BranchTrackingStatus.of(repository, branchName);
            if (trackingStatus == null) {
                ObjectId localHead = repository.resolve(Constants.HEAD);
                if (localHead == null) {
                    return false;
                }
                String targetRefName = Constants.R_HEADS + branchName;
                Collection<Ref> remoteHeads = git.lsRemote()
                    .setRemote("origin")
                    .setHeads(true)
                    .call();
                for (Ref remoteHead : remoteHeads) {
                    if (targetRefName.equals(remoteHead.getName()) && remoteHead.getObjectId() != null) {
                        return localHead.equals(remoteHead.getObjectId());
                    }
                }
                return false;
            }
            return trackingStatus.getAheadCount() == 0;
        } catch (IOException | GitAPIException ex) {
            throw new GitOperationException("Failed to determine push status", ex);
        }
    }

    @Override
    public String detectDefaultBranch() {
        try {
            Ref remoteHead = repository.exactRef(Constants.R_REMOTES + "origin/HEAD");
            if (remoteHead != null && remoteHead.isSymbolic() && remoteHead.getTarget() != null) {
                String target = remoteHead.getTarget().getName();
                if (target != null && target.startsWith(Constants.R_REMOTES + "origin/")) {
                    return target.substring((Constants.R_REMOTES + "origin/").length());
                }
            }

            if (repository.findRef(Constants.R_HEADS + "main") != null) {
                return "main";
            }
            if (repository.findRef(Constants.R_HEADS + "master") != null) {
                return "master";
            }
            return currentBranchName();
        } catch (IOException ex) {
            throw new GitOperationException("Failed to detect default branch", ex);
        }
    }

    @Override
    public void close() {
        git.close();
        repository.close();
    }

    private void checkoutRemoteBranch(String branchName, boolean force) {
        try {
            String remoteStartPoint = shortRemoteRef(branchName);
            String localBranchName = localBranchName(branchName);
            if (force) {
                git.reset().setMode(ResetCommand.ResetType.HARD).call();
            }
            git.checkout()
                .setCreateBranch(true)
                .setName(localBranchName)
                .setStartPoint(remoteStartPoint)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                .call();
        } catch (GitAPIException ex) {
            throw new GitOperationException("Failed to checkout branch " + branchName, ex);
        }
    }

    private void checkoutDetachedRef(String refName, boolean force) {
        try {
            if (force) {
                git.reset().setMode(ResetCommand.ResetType.HARD).call();
            }
            git.checkout().setName(refName).call();
        } catch (CheckoutConflictException conflictException) {
            if (!force) {
                throw new GitOperationException("Checkout blocked by local changes", conflictException);
            }
            forceCheckout(refName);
        } catch (GitAPIException ex) {
            throw new GitOperationException("Failed to checkout ref " + refName, ex);
        }
    }

    private void forceCheckout(String branchName) {
        try {
            git.reset().setMode(ResetCommand.ResetType.HARD).call();
            git.checkout().setName(branchName).call();
        } catch (GitAPIException ex) {
            throw new GitOperationException("Failed to force checkout branch " + branchName, ex);
        }
    }

    private void revertHeadCommit() throws IOException, GitAPIException {
        ObjectId headId = repository.resolve(Constants.HEAD);
        if (headId == null) {
            throw new GitOperationException("Repository does not have any commits");
        }
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit head = walk.parseCommit(headId);
            RevCommit revertResult = git.revert().include(head).call();
            if (revertResult == null) {
                throw new GitOperationException("Revert failed due to merge conflicts");
            }
        }
    }

    private void validatePushResults(Iterable<PushResult> results) {
        for (PushResult result : results) {
            Collection<RemoteRefUpdate> updates = result.getRemoteUpdates();
            for (RemoteRefUpdate update : updates) {
                RemoteRefUpdate.Status status = update.getStatus();
                if (status == RemoteRefUpdate.Status.OK || status == RemoteRefUpdate.Status.UP_TO_DATE) {
                    continue;
                }
                if (status == RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD) {
                    throw new GitOperationException("Push rejected: non-fast-forward. Pull and merge first.");
                }
                if (status == RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED) {
                    throw new GitOperationException("Push rejected: remote changed. Pull and rebase first.");
                }
                if (status == RemoteRefUpdate.Status.REJECTED_OTHER_REASON) {
                    throw new GitOperationException("Push rejected by remote: " + update.getMessage());
                }
                if (status == RemoteRefUpdate.Status.NON_EXISTING) {
                    throw new GitOperationException("Push failed: remote branch does not exist");
                }
                if (status == RemoteRefUpdate.Status.AWAITING_REPORT
                    || status == RemoteRefUpdate.Status.NOT_ATTEMPTED) {
                    throw new GitOperationException("Push failed: no remote status was reported");
                }
            }
        }
    }

    private String currentBranchName() throws IOException {
        if (isDetachedHead()) {
            ObjectId head = repository.resolve(Constants.HEAD);
            return head == null ? "HEAD" : head.getName().substring(0, 7);
        }
        String fullBranch = repository.getFullBranch();
        if (fullBranch == null) {
            return "HEAD";
        }
        return Repository.shortenRefName(fullBranch);
    }

    private String trackingLabel(String branchName) throws IOException {
        BranchTrackingStatus trackingStatus = BranchTrackingStatus.of(repository, branchName);
        if (trackingStatus == null || trackingStatus.getRemoteTrackingBranch() == null) {
            return "";
        }
        return Repository.shortenRefName(trackingStatus.getRemoteTrackingBranch());
    }

    private TagRef resolveCurrentTag(ObjectId headId) throws GitAPIException, IOException {
        if (headId == null) {
            return null;
        }
        for (TagRef tag : listTags()) {
            Ref ref = repository.findRef(tag.fullName());
            if (ref != null && headId.equals(resolveObjectId(ref))) {
                return tag;
            }
        }
        return null;
    }

    private ObjectId resolveObjectId(Ref ref) throws IOException {
        Ref peeled = repository.getRefDatabase().peel(ref);
        return peeled.getPeeledObjectId() != null ? peeled.getPeeledObjectId() : peeled.getObjectId();
    }

    private boolean isDetachedHead() throws IOException {
        String fullBranch = repository.getFullBranch();
        return fullBranch == null || !fullBranch.startsWith(Constants.R_HEADS);
    }

    private static CommitInfo toCommitInfo(RevCommit commit) {
        String hash = commit.getName();
        String shortHash = hash.length() > 7 ? hash.substring(0, 7) : hash;
        String message = commit.getShortMessage() == null ? "" : commit.getShortMessage();
        String author = commit.getAuthorIdent() == null ? "unknown" : commit.getAuthorIdent().getName();
        Instant timestamp = Instant.ofEpochSecond(commit.getCommitTime());
        return new CommitInfo(hash, shortHash, message, author, timestamp);
    }

    private static void validateBranchName(String branchName, String label) {
        if (branchName == null || branchName.isBlank()) {
            throw new GitOperationException(label + " is required");
        }
    }

    private static String shortRemoteRef(String branchName) {
        if (branchName.startsWith(Constants.R_REMOTES)) {
            return Repository.shortenRefName(branchName);
        }
        if (branchName.contains("/")) {
            return branchName;
        }
        return "origin/" + branchName;
    }

    private static String localBranchName(String branchName) {
        String remoteRef = shortRemoteRef(branchName);
        int slashIndex = remoteRef.indexOf('/');
        if (slashIndex < 0 || slashIndex == remoteRef.length() - 1) {
            return remoteRef;
        }
        return remoteRef.substring(slashIndex + 1);
    }

    private static String shortenLocalBranchName(String branchName) {
        if (branchName.startsWith(Constants.R_HEADS)) {
            return branchName.substring(Constants.R_HEADS.length());
        }
        return branchName;
    }

    private static String remoteName(String fullName) {
        if (!fullName.startsWith(Constants.R_REMOTES)) {
            return "";
        }
        String shortened = fullName.substring(Constants.R_REMOTES.length());
        int slashIndex = shortened.indexOf('/');
        if (slashIndex < 0) {
            return shortened;
        }
        return shortened.substring(0, slashIndex);
    }

    private static String remoteBranchName(String fullName) {
        String remoteName = remoteName(fullName);
        if (remoteName.isBlank()) {
            return Repository.shortenRefName(fullName);
        }
        String prefix = Constants.R_REMOTES + remoteName + "/";
        if (fullName.startsWith(prefix)) {
            return fullName.substring(prefix.length());
        }
        return Repository.shortenRefName(fullName);
    }

    private static Set<String> copySet(Set<String> source) {
        return Set.copyOf(new LinkedHashSet<>(source));
    }
}

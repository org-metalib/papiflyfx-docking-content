package org.metalib.papifly.fx.github.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.github.model.BranchRef;
import org.metalib.papifly.fx.github.model.CommitInfo;
import org.metalib.papifly.fx.github.model.CurrentRefState;
import org.metalib.papifly.fx.github.model.GitRefKind;
import org.metalib.papifly.fx.github.model.RepoStatus;
import org.metalib.papifly.fx.github.model.RollbackMode;
import org.metalib.papifly.fx.github.model.TagRef;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JGitRepositoryTest {

    @Test
    void loadsStatusAndBranches(@TempDir Path tempDir) throws Exception {
        Path repoDir = tempDir.resolve("repo");
        initRepository(repoDir);

        try (JGitRepository repository = new JGitRepository(repoDir, () -> null)) {
            RepoStatus status = repository.loadStatus();
            List<BranchRef> branches = repository.listBranches();

            assertNotNull(status.currentBranch());
            assertFalse(status.currentBranch().isBlank());
            assertFalse(status.dirty());
            assertFalse(branches.isEmpty());
            assertTrue(branches.stream().anyMatch(BranchRef::current));
        }
    }

    @Test
    void createsAndChecksOutBranchAndCommits(@TempDir Path tempDir) throws Exception {
        Path repoDir = tempDir.resolve("repo");
        initRepository(repoDir);

        try (JGitRepository repository = new JGitRepository(repoDir, () -> null)) {
            String startPoint = repository.loadStatus().currentBranch();
            repository.createAndCheckout("feature/test", startPoint);

            writeFile(repoDir.resolve("feature.txt"), "feature");
            CommitInfo commitInfo = repository.commitAll("feature commit");
            RepoStatus status = repository.loadStatus();

            assertEquals("feature/test", status.currentBranch());
            assertEquals("feature commit", commitInfo.message());
            assertFalse(status.dirty());
        }
    }

    @Test
    void supportsRollbackModes(@TempDir Path tempDir) throws Exception {
        Path repoDir = tempDir.resolve("repo");
        initRepository(repoDir);

        try (JGitRepository repository = new JGitRepository(repoDir, () -> null)) {
            String startPoint = repository.loadStatus().currentBranch();
            repository.createAndCheckout("feature/rollback", startPoint);

            writeFile(repoDir.resolve("rollback.txt"), "one");
            repository.commitAll("commit one");

            repository.rollback(RollbackMode.RESET_SOFT);
            assertTrue(repository.loadStatus().dirty());

            repository.commitAll("commit two");
            repository.rollback(RollbackMode.RESET_HARD);
            assertFalse(repository.loadStatus().dirty());

            writeFile(repoDir.resolve("rollback.txt"), "two");
            repository.commitAll("commit three");
            String beforeRevert = repository.getHeadCommit().hash();
            repository.rollback(RollbackMode.REVERT);
            String afterRevert = repository.getHeadCommit().hash();

            assertFalse(beforeRevert.equals(afterRevert));
        }
    }

    @Test
    void tracksHeadPushedState(@TempDir Path tempDir) throws Exception {
        Path remoteDir = tempDir.resolve("remote.git");
        Path localDir = tempDir.resolve("local");

        initBareRepository(remoteDir);
        initRepository(localDir);

        try (Git git = Git.open(localDir.toFile())) {
            String current = git.getRepository().getBranch();
            git.remoteAdd().setName("origin").setUri(new org.eclipse.jgit.transport.URIish(remoteDir.toUri().toString())).call();
            git.push().setRemote("origin").add("refs/heads/" + current + ":refs/heads/" + current).call();
        }

        try (JGitRepository repository = new JGitRepository(localDir, () -> null)) {
            assertTrue(repository.isHeadPushed());

            writeFile(localDir.resolve("new.txt"), "local");
            repository.commitAll("local commit");

            assertFalse(repository.isHeadPushed());
        }
    }

    @Test
    void mapsNonFastForwardPushRejection(@TempDir Path tempDir) throws Exception {
        Path remoteDir = tempDir.resolve("remote.git");
        Path seedDir = tempDir.resolve("seed");
        Path cloneADir = tempDir.resolve("clone-a");
        Path cloneBDir = tempDir.resolve("clone-b");

        initBareRepository(remoteDir);
        initRepository(seedDir);
        try (Git seed = Git.open(seedDir.toFile())) {
            String current = seed.getRepository().getBranch();
            seed.remoteAdd().setName("origin").setUri(new org.eclipse.jgit.transport.URIish(remoteDir.toUri().toString())).call();
            seed.push().setRemote("origin").add("refs/heads/" + current + ":refs/heads/" + current).call();
        }

        cloneRepository(remoteDir, cloneADir);
        cloneRepository(remoteDir, cloneBDir);

        try (Git cloneA = Git.open(cloneADir.toFile())) {
            writeFile(cloneADir.resolve("a.txt"), "a");
            cloneA.add().addFilepattern(".").call();
            cloneA.commit().setMessage("A commit").call();
            cloneA.push().setRemote("origin").call();
        }

        try (JGitRepository cloneB = new JGitRepository(cloneBDir, () -> null)) {
            writeFile(cloneBDir.resolve("b.txt"), "b");
            cloneB.commitAll("B commit");

            GitOperationException exception = assertThrows(GitOperationException.class,
                () -> cloneB.push("origin"));

            String message = exception.getMessage();
            assertTrue(message.contains("non-fast-forward") || message.contains("Push failed"));
        }
    }

    @Test
    void detectsDefaultBranch(@TempDir Path tempDir) throws Exception {
        Path repoDir = tempDir.resolve("repo");
        initRepository(repoDir);

        try (JGitRepository repository = new JGitRepository(repoDir, () -> null)) {
            String defaultBranch = repository.detectDefaultBranch();

            assertNotNull(defaultBranch);
            assertFalse(defaultBranch.isBlank());
        }
    }

    @Test
    void listsTagsAndResolvesDetachedTag(@TempDir Path tempDir) throws Exception {
        Path repoDir = tempDir.resolve("repo");
        initRepository(repoDir);

        try (Git git = Git.open(repoDir.toFile())) {
            git.tag().setName("v1.0.0").call();
            git.checkout().setName("v1.0.0").call();
        }

        try (JGitRepository repository = new JGitRepository(repoDir, () -> null)) {
            List<TagRef> tags = repository.listTags();
            CurrentRefState currentRef = repository.loadCurrentRef();

            assertTrue(tags.stream().anyMatch(tag -> tag.name().equals("v1.0.0") && tag.current()));
            assertEquals(GitRefKind.TAG, currentRef.kind());
            assertEquals("v1.0.0", currentRef.displayName());
            assertTrue(currentRef.detached());
        }
    }

    @Test
    void resolvesDetachedCommitWhenHeadIsNotOnTag(@TempDir Path tempDir) throws Exception {
        Path repoDir = tempDir.resolve("repo");
        initRepository(repoDir);
        String firstCommit;

        try (Git git = Git.open(repoDir.toFile())) {
            firstCommit = git.getRepository().resolve(Constants.HEAD).getName();
            writeFile(repoDir.resolve("commit.txt"), "two");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("second").call();
            git.checkout().setName(firstCommit).call();
        }

        try (JGitRepository repository = new JGitRepository(repoDir, () -> null)) {
            CurrentRefState currentRef = repository.loadCurrentRef();

            assertEquals(GitRefKind.DETACHED_COMMIT, currentRef.kind());
            assertEquals(firstCommit.substring(0, 7), currentRef.displayName());
            assertTrue(currentRef.detached());
        }
    }

    @Test
    void updateFetchesRemoteChangesWithoutMerging(@TempDir Path tempDir) throws Exception {
        Path remoteDir = tempDir.resolve("remote.git");
        Path seedDir = tempDir.resolve("seed");
        Path localDir = tempDir.resolve("local");

        initBareRepository(remoteDir);
        initRepository(seedDir);
        try (Git seed = Git.open(seedDir.toFile())) {
            String current = seed.getRepository().getBranch();
            seed.remoteAdd().setName("origin").setUri(new org.eclipse.jgit.transport.URIish(remoteDir.toUri().toString())).call();
            seed.push().setRemote("origin").add("refs/heads/" + current + ":refs/heads/" + current).call();
        }

        cloneRepository(remoteDir, localDir);

        try (Git seed = Git.open(seedDir.toFile())) {
            writeFile(seedDir.resolve("update.txt"), "remote");
            seed.add().addFilepattern(".").call();
            seed.commit().setMessage("remote update").call();
            seed.push().setRemote("origin").call();
        }

        try (JGitRepository repository = new JGitRepository(localDir, () -> null)) {
            repository.update();
            RepoStatus status = repository.loadStatus();

            assertEquals(1, status.behindCount());
            assertEquals(0, status.aheadCount());
        }
    }

    private static void initRepository(Path directory) throws Exception {
        Files.createDirectories(directory);
        try (Git git = Git.init().setDirectory(directory.toFile()).call()) {
            writeFile(directory.resolve("README.md"), "seed");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("init").setAuthor("test", "test@example.com").call();
        }
    }

    private static void initBareRepository(Path directory) throws Exception {
        Files.createDirectories(directory);
        try (Git ignored = Git.init().setBare(true).setDirectory(directory.toFile()).call()) {
        }
    }

    private static void cloneRepository(Path remoteDir, Path destination) throws Exception {
        try (Git ignored = Git.cloneRepository()
            .setURI(remoteDir.toUri().toString())
            .setDirectory(destination.toFile())
            .call()) {
        }
    }

    private static void writeFile(Path path, String text) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, text);
    }
}

# papiflyfx-docking-github — Implementation Plan (Sonnet)

> Synthesised from ChatGPT, Gemini, and Grok research; cross-referenced with the
> existing opus and codex plans; aligned to the PapiflyFX docking architecture
> (v 0.0.14-SNAPSHOT).

---

## 1. Module Identity

| Field         | Value                                               |
|---------------|-----------------------------------------------------|
| artifactId    | `papiflyfx-docking-github`                          |
| groupId       | `org.metalib.papifly.docking`                       |
| Java package  | `org.metalib.papifly.fx.github`                     |
| Sub-packages  | `.model`, `.auth`, `.git`, `.github`, `.ui`, `.ui.dialog` |

**Dependency flow:**

```
papiflyfx-docking-api   ←  papiflyfx-docking-github  (compile)
papiflyfx-docking-docks ←  papiflyfx-docking-github  (test scope only)
JGit                    ←  papiflyfx-docking-github  (compile)
javafx-controls         ←  papiflyfx-docking-github  (compile, transitive from api)
```

No external JSON library is used. All GitHub REST serialisation follows the
project convention of hand-rolled `java.util.Map`-based JSON (consistent with
`DockSessionSerializer`) via `java.net.http.HttpClient`.

---

## 2. Maven POM

All dependency versions live in the parent POM. The module child POM only
references managed coordinates.

### 2.1 Root `pom.xml` additions

```xml
<!-- <modules> section -->
<module>papiflyfx-docking-github</module>

<!-- <properties> section -->
<jgit.version>7.2.0.202503040940-r</jgit.version>

<!-- <dependencyManagement> section -->
<dependency>
    <groupId>org.eclipse.jgit</groupId>
    <artifactId>org.eclipse.jgit</artifactId>
    <version>${jgit.version}</version>
</dependency>
```

### 2.2 `papiflyfx-docking-github/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.metalib.papifly.docking</groupId>
        <artifactId>papiflyfx-docking</artifactId>
        <version>0.0.14-SNAPSHOT</version>
    </parent>

    <artifactId>papiflyfx-docking-github</artifactId>
    <name>papiflyfx-docking-github</name>
    <description>Dockable GitHub/Git toolbar for PapiflyFX docking.</description>

    <properties>
        <testfx.headless>true</testfx.headless>
        <testfx.robot>glass</testfx.robot>
        <testfx.platform>Desktop</testfx.platform>
        <monocle.platform>Headless</monocle.platform>
        <prism.order>sw</prism.order>
        <prism.text>t2k</prism.text>
        <java.awt.headless>false</java.awt.headless>
    </properties>

    <dependencies>
        <!-- API contract -->
        <dependency>
            <groupId>org.metalib.papifly.docking</groupId>
            <artifactId>papiflyfx-docking-api</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- JavaFX -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-base</artifactId>
            <classifier>${javafx.platform}</classifier>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-graphics</artifactId>
            <classifier>${javafx.platform}</classifier>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <classifier>${javafx.platform}</classifier>
        </dependency>

        <!-- JGit — local Git operations -->
        <dependency>
            <groupId>org.eclipse.jgit</groupId>
            <artifactId>org.eclipse.jgit</artifactId>
        </dependency>

        <!-- Docks — test scope only (integration / UI tests) -->
        <dependency>
            <groupId>org.metalib.papifly.docking</groupId>
            <artifactId>papiflyfx-docking-docks</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>

        <!-- Test stack -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testfx</groupId>
            <artifactId>testfx-junit5</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testfx</groupId>
            <artifactId>testfx-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testfx</groupId>
            <artifactId>openjfx-monocle</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <useModulePath>false</useModulePath>
                    <argLine>
                        --enable-native-access=javafx.graphics
                        --add-exports=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
                        --add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
                        --add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED
                        --add-opens=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED
                        --add-exports=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED
                        --add-opens=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED
                        --add-exports=javafx.base/com.sun.javafx.logging=ALL-UNNAMED
                        --add-opens=javafx.base/com.sun.javafx.logging=ALL-UNNAMED
                    </argLine>
                    <systemPropertyVariables>
                        <testfx.headless>${testfx.headless}</testfx.headless>
                        <testfx.robot>${testfx.robot}</testfx.robot>
                        <testfx.platform>${testfx.platform}</testfx.platform>
                        <monocle.platform>${monocle.platform}</monocle.platform>
                        <prism.order>${prism.order}</prism.order>
                        <prism.text>${prism.text}</prism.text>
                        <java.awt.headless>${java.awt.headless}</java.awt.headless>
                    </systemPropertyVariables>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <profile>
            <id>headless-tests</id>
            <activation>
                <property>
                    <name>testfx.headless</name>
                    <value>true</value>
                </property>
            </activation>
            <properties>
                <testfx.platform>Monocle</testfx.platform>
                <testfx.robot>glass</testfx.robot>
                <monocle.platform>Headless</monocle.platform>
                <prism.order>sw</prism.order>
                <prism.text>t2k</prism.text>
                <java.awt.headless>true</java.awt.headless>
            </properties>
        </profile>
    </profiles>
</project>
```

---

## 3. Architecture Overview

The module follows a strict four-layer design. No layer reaches down more than one
level.

```
┌─────────────────────────────────────────────────────────────────────┐
│  UI Layer                                                           │
│  GitHubToolbar (HBox)  ←binds→  GitHubToolbarViewModel             │
│  Dialogs: CommitDialog, NewBranchDialog, RollbackDialog, PRDialog   │
│  DirtyCheckoutGuard (inline alert)                                  │
└─────────────────────────┬──────────────────┬────────────────────────┘
                          │                  │
          ┌───────────────▼──┐    ┌──────────▼────────────┐
          │  Git Layer        │    │  GitHub API Layer      │
          │  GitRepository    │    │  GitHubApiService      │
          │  (JGit wrapper)   │    │  (java.net.http)       │
          │                   │    │  RemoteUrlParser       │
          └───────────────────┘    └───────────────────────┘
                          │
          ┌───────────────▼──────────────┐
          │  Auth Layer                   │
          │  CredentialStore (interface)  │
          │  PatCredentialStore           │
          │  PreferencesTokenStore        │
          └──────────────────────────────┘
```

### 3.1 Design principles

1. **UI contains zero JGit or HTTP calls.** All domain calls go via the ViewModel.
2. **ViewModel is the single source of truth** for all observable state.
3. **All slow operations run off the FX thread** using `CommandRunner` backed by a
   single-threaded daemon `ExecutorService`.
4. **Services return immutable value objects** (records) or throw typed
   `GitOperationException` / `GitHubApiException`.
5. **No FXML, no CSS** — all JavaFX nodes are built programmatically per project convention.
6. **Theme binding** — toolbar reacts to `dm.themeProperty()` in real time.

### 3.2 Runtime state machine

```
UNINITIALIZED
    ──(open repo)──►  READY
    ──(open fails)──► ERROR

READY
    ──(start command)──► BUSY

BUSY
    ──(success)──► READY
    ──(auth err)──► AUTH_REQUIRED
    ──(hard err)──► ERROR

AUTH_REQUIRED
    ──(token saved)──► READY
    ──(cancelled)───► READY (auth-guarded buttons remain disabled)
```

---

## 4. Package & File Layout

```
papiflyfx-docking-github/
├── pom.xml
└── src/
    ├── main/java/org/metalib/papifly/fx/github/
    │   ├── GitHubRepoContext.java          ← config record (host → toolbar)
    │   ├── GitHubToolbar.java              ← top-level public HBox Node
    │   ├── GitHubToolbarContribution.java  ← mount helper (top/bottom)
    │   │
    │   ├── auth/
    │   │   ├── CredentialStore.java        ← token read/write interface
    │   │   ├── PatCredentialStore.java     ← in-memory PAT impl
    │   │   └── PreferencesTokenStore.java  ← java.util.prefs persistence
    │   │
    │   ├── model/
    │   │   ├── BranchRef.java
    │   │   ├── CommitInfo.java
    │   │   ├── RepoStatus.java
    │   │   ├── PullRequestDraft.java
    │   │   ├── PullRequestResult.java
    │   │   └── RollbackMode.java
    │   │
    │   ├── git/
    │   │   ├── GitRepository.java          ← interface
    │   │   ├── JGitRepository.java         ← JGit implementation
    │   │   └── GitOperationException.java
    │   │
    │   ├── github/
    │   │   ├── GitHubApiService.java
    │   │   └── RemoteUrlParser.java
    │   │
    │   └── ui/
    │       ├── GitHubToolbarViewModel.java
    │       ├── CommandRunner.java
    │       ├── StatusBar.java
    │       └── dialog/
    │           ├── CommitDialog.java
    │           ├── NewBranchDialog.java
    │           ├── RollbackDialog.java
    │           ├── PullRequestDialog.java
    │           ├── TokenDialog.java
    │           └── DirtyCheckoutAlert.java
    │
    └── test/java/org/metalib/papifly/fx/github/
        ├── RemoteUrlParserTest.java
        ├── GitHubRepoContextTest.java
        ├── GitHubToolbarFxTest.java
        ├── git/
        │   └── JGitRepositoryTest.java
        ├── github/
        │   └── GitHubApiServiceTest.java
        └── ui/
            └── GitHubToolbarViewModelTest.java
```

---

## 5. Model Layer (Records)

### 5.1 `GitHubRepoContext`

The configuration record supplied by the host application. `localClonePath` is
nullable; when absent the toolbar operates in *remote-only* mode (repo link, PR
creation via a known token, but no branch/commit/push).

```java
package org.metalib.papifly.fx.github;

import java.net.URI;
import java.nio.file.Path;

/**
 * Immutable configuration for the GitHub toolbar.
 *
 * @param remoteUrl      HTTPS or SSH URL of the remote repository (required)
 * @param localClonePath local working-copy root; null → remote-only mode
 * @param owner          GitHub account name (parsed from remoteUrl)
 * @param repo           GitHub repository name (parsed from remoteUrl)
 */
public record GitHubRepoContext(
        URI remoteUrl,
        Path localClonePath,
        String owner,
        String repo
) {
    public static GitHubRepoContext of(URI remoteUrl, Path localClonePath) {
        var coords = RemoteUrlParser.parse(remoteUrl.toString());
        return new GitHubRepoContext(remoteUrl, localClonePath, coords.owner(), coords.repo());
    }

    public static GitHubRepoContext remoteOnly(URI remoteUrl) {
        return of(remoteUrl, null);
    }

    public boolean hasLocalClone() {
        return localClonePath != null;
    }

    /** Fully-qualified HTTPS URL for opening in a browser. */
    public String htmlUrl() {
        return "https://github.com/" + owner + "/" + repo;
    }
}
```

### 5.2 `BranchRef`

```java
package org.metalib.papifly.fx.github.model;

/**
 * A lightweight snapshot of a single Git branch reference.
 *
 * @param name      short ref name (e.g. "feature/login", "origin/main")
 * @param isRemote  true for remote-tracking refs
 * @param isCurrent true when this branch is currently checked out
 */
public record BranchRef(String name, boolean isRemote, boolean isCurrent) {}
```

### 5.3 `RepoStatus`

```java
package org.metalib.papifly.fx.github.model;

import java.util.Set;

/**
 * Snapshot of the working-tree status.
 */
public record RepoStatus(
        String currentBranch,
        String defaultBranch,
        boolean detachedHead,
        boolean aheadOfRemote,
        Set<String> added,
        Set<String> changed,
        Set<String> removed,
        Set<String> untracked,
        Set<String> conflicting
) {
    public boolean isDirty() {
        return !(added.isEmpty() && changed.isEmpty() && removed.isEmpty()
                && untracked.isEmpty() && conflicting.isEmpty());
    }

    public int trackedChangedCount() {
        return added.size() + changed.size() + removed.size();
    }
}
```

### 5.4 `CommitInfo`

```java
package org.metalib.papifly.fx.github.model;

import java.time.Instant;

/**
 * Metadata for a single Git commit.
 *
 * @param isPushed best-effort: true when tracking status shows 0 ahead count
 */
public record CommitInfo(
        String hash,
        String shortHash,
        String message,
        String author,
        Instant timestamp,
        boolean isPushed
) {}
```

### 5.5 `RollbackMode`

```java
package org.metalib.papifly.fx.github.model;

/**
 * Strategy for undoing the last commit.
 *
 * <ul>
 *   <li>REVERT      — new commit that reverses the last; safe for pushed commits</li>
 *   <li>RESET_SOFT  — move HEAD back one; keep changes staged (local-only)</li>
 *   <li>RESET_HARD  — move HEAD back one; discard all changes (destructive)</li>
 * </ul>
 */
public enum RollbackMode {
    REVERT,
    RESET_SOFT,
    RESET_HARD
}
```

### 5.6 `PullRequestDraft` and `PullRequestResult`

```java
package org.metalib.papifly.fx.github.model;

public record PullRequestDraft(
        String title,
        String body,
        String headBranch,
        String baseBranch,
        boolean openInBrowser
) {}

public record PullRequestResult(int number, String htmlUrl) {}
```

---

## 6. Auth Layer

### 6.1 `CredentialStore` interface

```java
package org.metalib.papifly.fx.github.auth;

import java.util.Optional;

/**
 * Abstraction for secure PAT storage.
 * Phase 1 ships with a simple PAT implementation.
 * A future phase may add OS keychain or OAuth device-flow.
 */
public interface CredentialStore {
    Optional<String> getToken();
    void setToken(String token);
    void clearToken();

    default boolean isAuthenticated() {
        return getToken().isPresent();
    }

    /** Returns a JGit-compatible credentials provider for push operations. */
    default org.eclipse.jgit.transport.CredentialsProvider toJGitCredentials() {
        return getToken()
                .map(t -> (org.eclipse.jgit.transport.CredentialsProvider)
                        new org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(t, ""))
                .orElse(null);
    }
}
```

### 6.2 `PatCredentialStore` — in-memory

```java
package org.metalib.papifly.fx.github.auth;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe in-memory PAT store. Token is not persisted across JVM restarts.
 * Use {@link PreferencesTokenStore} for persistence.
 */
public class PatCredentialStore implements CredentialStore {

    private final AtomicReference<String> token = new AtomicReference<>();

    public PatCredentialStore() {}

    public PatCredentialStore(String initialToken) {
        this.token.set(initialToken);
    }

    @Override public Optional<String> getToken()      { return Optional.ofNullable(token.get()); }
    @Override public void setToken(String t)          { token.set(t); }
    @Override public void clearToken()                { token.set(null); }
}
```

### 6.3 `PreferencesTokenStore` — persistent via `java.util.prefs`

Token is persisted to the system user preferences store. For production use,
replace with OS keychain integration.

```java
package org.metalib.papifly.fx.github.auth;

import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Persists the PAT in the system user Preferences node.
 * <p>
 * Note: this is convenience persistence only.
 * For production hardening replace with OS keychain integration.
 */
public class PreferencesTokenStore implements CredentialStore {

    private static final String KEY = "pat";
    private final Preferences prefs =
            Preferences.userRoot().node("org/metalib/papifly/github");

    @Override
    public Optional<String> getToken() {
        return Optional.ofNullable(prefs.get(KEY, null));
    }

    @Override
    public void setToken(String token) {
        prefs.put(KEY, token);
    }

    @Override
    public void clearToken() {
        prefs.remove(KEY);
    }
}
```

---

## 7. Remote URL Parser

Handles the three URL forms GitHub produces:
1. `https://github.com/owner/repo.git`
2. `git@github.com:owner/repo.git` (SCP style)
3. `ssh://git@github.com/owner/repo.git`

```java
package org.metalib.papifly.fx.github.github;

import java.net.URI;

/**
 * Parses GitHub remote URLs into owner/repo coordinates.
 */
public final class RemoteUrlParser {

    public record RemoteCoordinates(String owner, String repo) {}

    private RemoteUrlParser() {}

    public static RemoteCoordinates parse(String remote) {
        String s = remote.trim();
        if (s.endsWith(".git")) {
            s = s.substring(0, s.length() - 4);
        }

        // SCP style: git@github.com:owner/repo
        if (s.startsWith("git@github.com:")) {
            String pair = s.substring("git@github.com:".length());
            return splitOwnerRepo(pair, remote);
        }

        // SSH URI: ssh://git@github.com/owner/repo
        if (s.startsWith("ssh://")) {
            s = s.replace("ssh://git@github.com/", "https://github.com/");
        }

        // HTTPS: https://github.com/owner/repo
        URI uri = URI.create(s);
        String path = uri.getPath().replaceFirst("^/", "");
        return splitOwnerRepo(path, remote);
    }

    private static RemoteCoordinates splitOwnerRepo(String path, String original) {
        String[] parts = path.split("/", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException("Cannot parse owner/repo from: " + original);
        }
        return new RemoteCoordinates(parts[0], parts[1]);
    }
}
```

---

## 8. Git Layer (JGit)

### 8.1 `GitRepository` interface

Defines the boundary between ViewModel and JGit. Makes the domain testable
without a real filesystem.

```java
package org.metalib.papifly.fx.github.git;

import org.metalib.papifly.fx.github.model.*;

import java.util.List;

public interface GitRepository extends AutoCloseable {
    RepoStatus loadStatus();
    List<BranchRef> listBranches();
    void checkout(String branchName, boolean force);
    void createAndCheckout(String branchName, String startPoint);
    CommitInfo commitAll(String message);
    CommitInfo getHeadCommit();
    void rollback(RollbackMode mode);
    void push(String remoteName);
    boolean isHeadPushed();
    String detectDefaultBranch();
    @Override void close();
}
```

### 8.2 `GitOperationException`

```java
package org.metalib.papifly.fx.github.git;

public class GitOperationException extends RuntimeException {
    public GitOperationException(String message, Throwable cause) {
        super(message, cause);
    }
    public GitOperationException(String message) {
        super(message);
    }
}
```

### 8.3 `JGitRepository` — full implementation

```java
package org.metalib.papifly.fx.github.git;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.metalib.papifly.fx.github.model.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

public class JGitRepository implements GitRepository {

    private final Git git;
    private final CredentialsProvider credentials;

    public JGitRepository(Path workingDir, CredentialsProvider credentials) {
        try {
            Repository repo = new org.eclipse.jgit.storage.file.FileRepositoryBuilder()
                    .findGitDir(workingDir.toFile())
                    .build();
            this.git = new Git(repo);
            this.credentials = credentials;
        } catch (IOException ex) {
            throw new GitOperationException("Cannot open repository at: " + workingDir, ex);
        }
    }

    // ── Status & branch listing ───────────────────────────────────────────────

    @Override
    public RepoStatus loadStatus() {
        try {
            String branch = git.getRepository().getBranch();
            boolean detached = branch == null || branch.length() == 40; // SHA implies detached
            Status s = git.status().call();

            String defaultBranch = detectDefaultBranch();
            boolean ahead = isHeadPushed(); // reuse: false means ahead

            return new RepoStatus(
                    detached ? "(detached)" : branch,
                    defaultBranch,
                    detached,
                    !ahead,
                    s.getAdded(),
                    s.getChanged(),
                    s.getRemoved(),
                    s.getUntracked(),
                    s.getConflicting()
            );
        } catch (IOException | GitAPIException ex) {
            throw new GitOperationException("Failed to load repository status", ex);
        }
    }

    @Override
    public List<BranchRef> listBranches() {
        try {
            String current = git.getRepository().getBranch();
            List<Ref> local = git.branchList().call();
            List<Ref> remote = git.branchList()
                    .setListMode(ListBranchCommand.ListMode.REMOTE)
                    .call();

            Stream<BranchRef> localRefs = local.stream()
                    .map(r -> {
                        String name = Repository.shortenRefName(r.getName());
                        return new BranchRef(name, false, name.equals(current));
                    });
            Stream<BranchRef> remoteRefs = remote.stream()
                    .filter(r -> !r.getName().endsWith("/HEAD"))
                    .map(r -> new BranchRef(Repository.shortenRefName(r.getName()), true, false));

            return Stream.concat(localRefs, remoteRefs)
                    .sorted(Comparator.comparing(BranchRef::name))
                    .toList();
        } catch (IOException | GitAPIException ex) {
            throw new GitOperationException("Failed to list branches", ex);
        }
    }

    // ── Branch operations ────────────────────────────────────────────────────

    @Override
    public void checkout(String branchName, boolean force) {
        try {
            CheckoutCommand cmd = git.checkout().setName(branchName);
            if (force) cmd.setForced(true);
            cmd.call();
        } catch (GitAPIException ex) {
            throw new GitOperationException("Failed to checkout branch: " + branchName, ex);
        }
    }

    @Override
    public void createAndCheckout(String branchName, String startPoint) {
        try {
            git.checkout()
               .setCreateBranch(true)
               .setName(branchName)
               .setStartPoint(startPoint)
               .call();
        } catch (GitAPIException ex) {
            throw new GitOperationException("Failed to create branch: " + branchName, ex);
        }
    }

    // ── Commit ───────────────────────────────────────────────────────────────

    @Override
    public CommitInfo commitAll(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Commit message must not be blank");
        }
        try {
            // Stage modified/deleted tracked files
            git.add().addFilepattern(".").call();
            git.add().addFilepattern(".").setUpdate(true).call();
            RevCommit rev = git.commit().setMessage(message.strip()).call();
            return toCommitInfo(rev, false);
        } catch (GitAPIException ex) {
            throw new GitOperationException("Commit failed", ex);
        }
    }

    @Override
    public CommitInfo getHeadCommit() {
        try {
            Iterator<RevCommit> it = git.log().setMaxCount(1).call().iterator();
            if (!it.hasNext()) throw new GitOperationException("No commits found");
            return toCommitInfo(it.next(), isHeadPushed());
        } catch (GitAPIException ex) {
            throw new GitOperationException("Failed to read HEAD commit", ex);
        }
    }

    // ── Rollback ─────────────────────────────────────────────────────────────

    /**
     * Undo the last commit according to the chosen mode.
     *
     * Safety contract:
     * - REVERT always creates a new commit — safe for already-pushed commits.
     * - RESET_SOFT / RESET_HARD rewrite local history — must only be offered
     *   when HEAD is not yet pushed.
     */
    @Override
    public void rollback(RollbackMode mode) {
        try {
            ObjectId head = git.getRepository().resolve("HEAD");
            if (head == null) throw new GitOperationException("Nothing to roll back");

            switch (mode) {
                case REVERT -> git.revert().include(head).call();
                case RESET_SOFT -> git.reset()
                        .setMode(ResetCommand.ResetType.SOFT)
                        .setRef("HEAD~1")
                        .call();
                case RESET_HARD -> git.reset()
                        .setMode(ResetCommand.ResetType.HARD)
                        .setRef("HEAD~1")
                        .call();
            }
        } catch (GitAPIException | IOException ex) {
            throw new GitOperationException("Rollback failed", ex);
        }
    }

    // ── Push ─────────────────────────────────────────────────────────────────

    @Override
    public void push(String remoteName) {
        try {
            Iterable<org.eclipse.jgit.transport.PushResult> results = git.push()
                    .setRemote(remoteName)
                    .setCredentialsProvider(credentials)
                    .call();
            for (var result : results) {
                for (var update : result.getRemoteUpdates()) {
                    var status = update.getStatus();
                    if (status == org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD) {
                        throw new GitOperationException("Push rejected: non-fast-forward. Pull first.");
                    }
                    if (status == org.eclipse.jgit.transport.RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED) {
                        throw new GitOperationException("Push rejected: remote was updated. Pull first.");
                    }
                }
            }
        } catch (GitAPIException ex) {
            throw new GitOperationException("Push failed", ex);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @Override
    public boolean isHeadPushed() {
        try {
            String branch = git.getRepository().getBranch();
            if (branch == null) return false;
            BranchTrackingStatus tracking =
                    BranchTrackingStatus.of(git.getRepository(), branch);
            return tracking != null && tracking.getAheadCount() == 0;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Determines the default branch using a priority chain:
     *
     * 1. Remote HEAD symbolic ref via ls-remote (requires network).
     * 2. Local branch named "main".
     * 3. Local branch named "master".
     * 4. Current branch (last resort).
     */
    @Override
    public String detectDefaultBranch() {
        // Try remote HEAD (may be offline — swallow exceptions)
        try {
            Map<String, Ref> remoteRefs = git.lsRemote()
                    .setCredentialsProvider(credentials)
                    .callAsMap();
            Ref head = remoteRefs.get("HEAD");
            if (head != null && head.getTarget() != null) {
                return Repository.shortenRefName(head.getTarget().getName());
            }
        } catch (Exception ignored) {}

        // Local heuristic
        try {
            Set<String> locals = new HashSet<>();
            for (Ref r : git.branchList().call()) {
                locals.add(Repository.shortenRefName(r.getName()));
            }
            if (locals.contains("main")) return "main";
            if (locals.contains("master")) return "master";
            return git.getRepository().getBranch();
        } catch (IOException | GitAPIException ex) {
            return "main"; // safe fallback
        }
    }

    @Override
    public void close() {
        git.close();
    }

    private CommitInfo toCommitInfo(RevCommit rev, boolean pushed) {
        return new CommitInfo(
                rev.getName(),
                rev.abbreviate(7).name(),
                rev.getShortMessage(),
                rev.getAuthorIdent().getName(),
                Instant.ofEpochSecond(rev.getCommitTime()),
                pushed
        );
    }
}
```

---

## 9. GitHub API Layer

### 9.1 `GitHubApiService`

Uses `java.net.http.HttpClient` and hand-rolled JSON extraction — no extra
library dependency per project convention.

```java
package org.metalib.papifly.fx.github.github;

import org.metalib.papifly.fx.github.auth.CredentialStore;
import org.metalib.papifly.fx.github.model.PullRequestDraft;
import org.metalib.papifly.fx.github.model.PullRequestResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thin GitHub REST API v3 client.
 * Requires no JSON library — uses regex extraction on known response shapes.
 */
public class GitHubApiService {

    private static final String BASE = "https://api.github.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    private final CredentialStore credentials;

    public GitHubApiService(CredentialStore credentials) {
        this.credentials = credentials;
    }

    // ── Repository metadata ──────────────────────────────────────────────────

    /**
     * Fetches the configured default branch from GitHub.
     *
     * @throws GitHubApiException on HTTP errors or network failure
     */
    public String fetchDefaultBranch(String owner, String repo) {
        try {
            String body = get("/repos/%s/%s".formatted(owner, repo));
            String branch = extractString(body, "default_branch");
            if (branch == null) throw new GitHubApiException("No default_branch in response");
            return branch;
        } catch (IOException | InterruptedException ex) {
            throw new GitHubApiException("Failed to fetch default branch", ex);
        }
    }

    // ── Pull Request ─────────────────────────────────────────────────────────

    /**
     * Creates a pull request and returns the result.
     * HEAD branch must already be pushed to the remote.
     */
    public PullRequestResult createPullRequest(String owner, String repo, PullRequestDraft draft) {
        String jsonBody = """
                {
                  "title": "%s",
                  "head":  "%s",
                  "base":  "%s",
                  "body":  "%s"
                }
                """.formatted(
                escape(draft.title()),
                escape(draft.headBranch()),
                escape(draft.baseBranch()),
                escape(draft.body() != null ? draft.body() : "")
        );
        try {
            String response = post("/repos/%s/%s/pulls".formatted(owner, repo), jsonBody);
            String url = extractString(response, "html_url");
            int number = extractInt(response, "number");
            if (url == null) throw new GitHubApiException("No html_url in PR response");
            return new PullRequestResult(number, url);
        } catch (IOException | InterruptedException ex) {
            throw new GitHubApiException("Failed to create pull request", ex);
        }
    }

    // ── HTTP helpers ─────────────────────────────────────────────────────────

    private String get(String path) throws IOException, InterruptedException {
        HttpRequest req = baseRequest(path).GET().build();
        return send(req);
    }

    private String post(String path, String body) throws IOException, InterruptedException {
        HttpRequest req = baseRequest(path)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();
        return send(req);
    }

    private String send(HttpRequest req) throws IOException, InterruptedException {
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        int code = resp.statusCode();
        if (code == 401 || code == 403) {
            throw new GitHubApiException("Authentication failed (HTTP " + code + ")");
        }
        if (code == 422) {
            throw new GitHubApiException("GitHub rejected the request: " + resp.body());
        }
        if (code < 200 || code >= 300) {
            throw new GitHubApiException("GitHub API error HTTP " + code + ": " + resp.body());
        }
        return resp.body();
    }

    private HttpRequest.Builder baseRequest(String path) {
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .timeout(TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28");
        credentials.getToken().ifPresent(t ->
                builder.header("Authorization", "Bearer " + t));
        return builder;
    }

    // ── JSON helpers (minimal, no library) ───────────────────────────────────

    static String extractString(String json, String key) {
        Matcher m = Pattern.compile(
                "\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"")
                .matcher(json);
        return m.find() ? m.group(1) : null;
    }

    static int extractInt(String json, String key) {
        Matcher m = Pattern.compile(
                "\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)")
                .matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
```

### 9.2 `GitHubApiException`

```java
package org.metalib.papifly.fx.github.github;

public class GitHubApiException extends RuntimeException {
    public GitHubApiException(String message) { super(message); }
    public GitHubApiException(String message, Throwable cause) { super(message, cause); }
}
```

---

## 10. ViewModel Layer

### 10.1 `CommandRunner` — async execution helper

Separates threading concerns from command business logic. Backed by a single
daemon thread so Git operations are naturally serialised.

```java
package org.metalib.papifly.fx.github.ui;

import javafx.application.Platform;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Executes blocking operations on a background thread and delivers results
 * back on the JavaFX Application Thread.
 */
public final class CommandRunner implements AutoCloseable {

    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "github-toolbar-io");
        t.setDaemon(true);
        return t;
    });

    /**
     * Run {@code supplier} off the FX thread, then dispatch result/error to FX thread.
     */
    public <T> void run(
            Supplier<T> supplier,
            Consumer<T> onSuccess,
            Consumer<Throwable> onError
    ) {
        CompletableFuture.supplyAsync(supplier, io)
                .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
                .exceptionally(ex -> {
                    Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
                    Platform.runLater(() -> onError.accept(cause));
                    return null;
                });
    }

    /** Convenience overload for void operations. */
    public void run(
            Runnable action,
            Runnable onSuccess,
            Consumer<Throwable> onError
    ) {
        run(() -> { action.run(); return null; }, ignored -> onSuccess.run(), onError);
    }

    @Override
    public void close() {
        io.shutdownNow();
    }
}
```

### 10.2 `GitHubToolbarViewModel`

The ViewModel owns all observable state, drives all command executions, and
enforces all business rules (commit guard, rollback mode selection, auth check).

```java
package org.metalib.papifly.fx.github.ui;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.metalib.papifly.fx.github.GitHubRepoContext;
import org.metalib.papifly.fx.github.auth.CredentialStore;
import org.metalib.papifly.fx.github.git.GitRepository;
import org.metalib.papifly.fx.github.git.JGitRepository;
import org.metalib.papifly.fx.github.github.GitHubApiService;
import org.metalib.papifly.fx.github.model.*;

import java.util.List;

public final class GitHubToolbarViewModel implements AutoCloseable {

    // ── Observable state ─────────────────────────────────────────────────────

    private final StringProperty currentBranch  = new SimpleStringProperty("");
    private final StringProperty defaultBranch  = new SimpleStringProperty("main");
    private final ObservableList<BranchRef> branches = FXCollections.observableArrayList();
    private final BooleanProperty dirty          = new SimpleBooleanProperty(false);
    private final BooleanProperty busy           = new SimpleBooleanProperty(false);
    private final BooleanProperty authenticated  = new SimpleBooleanProperty(false);
    private final BooleanProperty localAvailable = new SimpleBooleanProperty(false);
    private final StringProperty statusText      = new SimpleStringProperty("");
    private final StringProperty errorText       = new SimpleStringProperty("");
    private final ObjectProperty<CommitInfo> headCommit = new SimpleObjectProperty<>();

    // ── Derived bindings ─────────────────────────────────────────────────────

    /** True when the current branch is the default (commit must be disabled). */
    public final BooleanBinding onDefaultBranch = currentBranch.isEqualTo(defaultBranch);
    public final BooleanBinding commitDisabled  = onDefaultBranch.or(dirty.not()).or(busy);
    public final BooleanBinding pushDisabled    = localAvailable.not().or(authenticated.not()).or(busy);
    public final BooleanBinding prDisabled      = authenticated.not().or(onDefaultBranch).or(busy);
    public final BooleanBinding localDisabled   = localAvailable.not().or(busy);

    // ── Services ─────────────────────────────────────────────────────────────

    private final GitHubRepoContext context;
    private final CredentialStore credentialStore;
    private final GitHubApiService apiService;
    private final CommandRunner runner = new CommandRunner();
    private GitRepository gitRepo;

    public GitHubToolbarViewModel(GitHubRepoContext context, CredentialStore credentialStore) {
        this.context = context;
        this.credentialStore = credentialStore;
        this.apiService = new GitHubApiService(credentialStore);
        this.authenticated.set(credentialStore.isAuthenticated());

        if (context.hasLocalClone()) {
            try {
                gitRepo = new JGitRepository(
                        context.localClonePath(),
                        credentialStore.toJGitCredentials());
                localAvailable.set(true);
            } catch (Exception e) {
                errorText.set("Cannot open repository: " + e.getMessage());
            }
        }
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    public void refresh() {
        if (gitRepo == null) return;
        execute("Refreshing…", () -> {
            RepoStatus status = gitRepo.loadStatus();
            List<BranchRef> branchList = gitRepo.listBranches();
            CommitInfo head = gitRepo.getHeadCommit();
            return new Object[]{status, branchList, head};
        }, result -> {
            var status = (RepoStatus) ((Object[]) result)[0];
            var branchList = (List<BranchRef>) ((Object[]) result)[1];
            var head = (CommitInfo) ((Object[]) result)[2];
            currentBranch.set(status.currentBranch());
            defaultBranch.set(status.defaultBranch());
            dirty.set(status.isDirty());
            branches.setAll(branchList);
            headCommit.set(head);
        });
    }

    public void switchBranch(String name) {
        execute("Switching to " + name + "…",
                () -> { gitRepo.checkout(name, false); return null; },
                _ -> refresh());
    }

    public void createBranch(String name, String startPoint) {
        execute("Creating " + name + "…",
                () -> { gitRepo.createAndCheckout(name, startPoint); return null; },
                _ -> refresh());
    }

    public void commit(String message) {
        execute("Committing…",
                () -> gitRepo.commitAll(message),
                info -> {
                    headCommit.set((CommitInfo) info);
                    statusText.set("Committed " + ((CommitInfo) info).shortHash());
                    refresh();
                });
    }

    public void rollback(RollbackMode mode) {
        execute("Rolling back (" + mode + ")…",
                () -> { gitRepo.rollback(mode); return null; },
                _ -> refresh());
    }

    public void push() {
        execute("Pushing…",
                () -> { gitRepo.push("origin"); return null; },
                _ -> {
                    statusText.set("Pushed successfully");
                    refresh();
                });
    }

    public void createPullRequest(PullRequestDraft draft) {
        execute("Creating PR…",
                () -> apiService.createPullRequest(context.owner(), context.repo(), draft),
                result -> {
                    var pr = (PullRequestResult) result;
                    statusText.set("PR #" + pr.number() + " created");
                    if (draft.openInBrowser()) {
                        openInBrowser(pr.htmlUrl());
                    }
                });
    }

    public void saveToken(String token) {
        credentialStore.setToken(token);
        authenticated.set(true);
        // Re-create JGit credentials with new token if local repo is open
        if (gitRepo != null) {
            gitRepo.close();
            try {
                gitRepo = new JGitRepository(
                        context.localClonePath(),
                        credentialStore.toJGitCredentials());
            } catch (Exception e) {
                errorText.set("Failed to reinitialise repository: " + e.getMessage());
            }
        }
    }

    // ── Property accessors ───────────────────────────────────────────────────

    public StringProperty currentBranchProperty()  { return currentBranch; }
    public StringProperty defaultBranchProperty()  { return defaultBranch; }
    public ObservableList<BranchRef> getBranches()  { return branches; }
    public BooleanProperty dirtyProperty()          { return dirty; }
    public BooleanProperty busyProperty()           { return busy; }
    public BooleanProperty authenticatedProperty()  { return authenticated; }
    public BooleanProperty localAvailableProperty() { return localAvailable; }
    public StringProperty statusTextProperty()      { return statusText; }
    public StringProperty errorTextProperty()       { return errorText; }
    public ObjectProperty<CommitInfo> headCommitProperty() { return headCommit; }
    public GitHubRepoContext getContext()            { return context; }

    @Override
    public void close() {
        runner.close();
        if (gitRepo != null) gitRepo.close();
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private <T> void execute(String statusMsg, java.util.function.Supplier<T> action,
                             java.util.function.Consumer<T> onSuccess) {
        busy.set(true);
        statusText.set(statusMsg);
        errorText.set("");
        runner.run(
                action,
                result -> {
                    busy.set(false);
                    onSuccess.accept(result);
                },
                ex -> {
                    busy.set(false);
                    errorText.set(ex.getMessage());
                }
        );
    }

    private static void openInBrowser(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception ignored) {}
    }
}
```

---

## 11. UI Layer

### 11.1 Visual layout

```
[ GitHub icon + owner/repo link ] | [ branch ● ] [ branch combo ] [ Checkout ] [ New Branch… ] | [ Commit… ] [ Rollback… ] [ Push ] | [ Create PR… ]       ··· [ ⟳ ] [ status text ] [ error ]
```

Items are grouped by `Separator` nodes. A growing spacer pushes the status
section to the right.

### 11.2 `GitHubToolbar` — full programmatic HBox

```java
package org.metalib.papifly.fx.github;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.github.auth.CredentialStore;
import org.metalib.papifly.fx.github.model.*;
import org.metalib.papifly.fx.github.ui.GitHubToolbarViewModel;
import org.metalib.papifly.fx.github.ui.dialog.*;

import java.awt.Desktop;
import java.net.URI;

/**
 * Horizontal GitHub/Git operations toolbar.
 * Mount via {@code BorderPane.setTop(toolbar)} or {@code setBottom(toolbar)}.
 */
public class GitHubToolbar extends HBox {

    private final GitHubToolbarViewModel vm;

    // UI nodes
    private final Hyperlink    repoLink    = new Hyperlink();
    private final Label        branchLabel = new Label();
    private final Circle       dirtyDot    = new Circle(4, Color.LIMEGREEN);
    private final ComboBox<String> branchCombo = new ComboBox<>();
    private final Button checkoutBtn  = new Button("Checkout");
    private final Button newBranchBtn = new Button("New Branch…");
    private final Button commitBtn    = new Button("Commit…");
    private final Button rollbackBtn  = new Button("Rollback…");
    private final Button pushBtn      = new Button("Push");
    private final Button prBtn        = new Button("Create PR…");
    private final Button tokenBtn     = new Button("🔑");
    private final ProgressIndicator spinner = new ProgressIndicator(-1);
    private final Label statusLabel  = new Label();
    private final Label errorLabel   = new Label();

    public GitHubToolbar(GitHubRepoContext context,
                         CredentialStore credentialStore,
                         ObjectProperty<Theme> themeProperty) {
        this.vm = new GitHubToolbarViewModel(context, credentialStore);

        buildLayout();
        bindProperties();
        wireActions();
        applyTheme(themeProperty.get());
        themeProperty.addListener((_, _, t) -> applyTheme(t));

        vm.refresh();
    }

    private void buildLayout() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        setPadding(new Insets(3, 8, 3, 8));

        spinner.setMaxSize(14, 14);
        spinner.visibleProperty().bind(vm.busyProperty());
        errorLabel.setTextFill(Color.TOMATO);

        tokenBtn.setTooltip(new Tooltip("Configure GitHub token"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(
                repoLink,
                new Separator(),
                branchLabel, dirtyDot, branchCombo, checkoutBtn, newBranchBtn,
                new Separator(),
                commitBtn, rollbackBtn, pushBtn,
                new Separator(),
                prBtn, tokenBtn,
                spacer,
                spinner, statusLabel, errorLabel
        );
    }

    private void bindProperties() {
        var ctx = vm.getContext();
        repoLink.setText(ctx.owner() + "/" + ctx.repo());

        branchLabel.textProperty().bind(vm.currentBranchProperty());

        vm.dirtyProperty().addListener((_, _, dirty) ->
                dirtyDot.setFill(dirty ? Color.ORANGE : Color.LIMEGREEN));

        // populate branch combo from observable list
        vm.getBranches().addListener(
                (javafx.collections.ListChangeListener<BranchRef>) _ ->
                        branchCombo.getItems().setAll(
                                vm.getBranches().stream()
                                        .filter(b -> !b.isRemote())
                                        .map(BranchRef::name)
                                        .toList()));

        // Commit: disabled on default branch OR nothing to commit OR busy
        commitBtn.disableProperty().bind(vm.commitDisabled);

        // Push / PR: disabled without credentials
        pushBtn.disableProperty().bind(vm.pushDisabled);
        prBtn.disableProperty().bind(vm.prDisabled);

        // Local-only actions
        checkoutBtn.disableProperty().bind(vm.localDisabled);
        newBranchBtn.disableProperty().bind(vm.localDisabled);
        rollbackBtn.disableProperty().bind(vm.localDisabled.or(vm.busyProperty()));

        statusLabel.textProperty().bind(vm.statusTextProperty());
        errorLabel.textProperty().bind(vm.errorTextProperty());
    }

    private void wireActions() {
        repoLink.setOnAction(_ -> openInBrowser(vm.getContext().htmlUrl()));

        checkoutBtn.setOnAction(_ -> {
            String selected = branchCombo.getValue();
            if (selected == null) return;
            if (vm.dirtyProperty().get()) {
                DirtyCheckoutAlert.show(selected).ifPresent(force -> {
                    if (force) vm.switchBranch(selected);
                });
            } else {
                vm.switchBranch(selected);
            }
        });

        newBranchBtn.setOnAction(_ ->
                NewBranchDialog.show(vm.currentBranchProperty().get())
                        .ifPresent(r -> vm.createBranch(r.name(), r.startPoint())));

        commitBtn.setOnAction(_ ->
                CommitDialog.show().ifPresent(vm::commit));

        rollbackBtn.setOnAction(_ -> {
            CommitInfo head = vm.headCommitProperty().get();
            if (head != null) {
                RollbackDialog.show(head).ifPresent(vm::rollback);
            }
        });

        pushBtn.setOnAction(_ -> vm.push());

        prBtn.setOnAction(_ ->
                PullRequestDialog.show(
                        vm.currentBranchProperty().get(),
                        vm.defaultBranchProperty().get()
                ).ifPresent(vm::createPullRequest));

        tokenBtn.setOnAction(_ ->
                TokenDialog.show().ifPresent(vm::saveToken));
    }

    private void applyTheme(Theme theme) {
        setStyle("-fx-background-color: " + toHex(theme.headerBackground()) + ";");
        branchLabel.setTextFill(theme.textColor());
        statusLabel.setTextFill(theme.textColor());
        repoLink.setTextFill(theme.accentColor());
    }

    private static String toHex(Color c) {
        return "#%02x%02x%02x".formatted(
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    private static void openInBrowser(String url) {
        try { Desktop.getDesktop().browse(URI.create(url)); }
        catch (Exception ignored) {}
    }

    public void dispose() {
        vm.close();
    }
}
```

### 11.3 `GitHubToolbarContribution` — mount helper

```java
package org.metalib.papifly.fx.github;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.github.auth.CredentialStore;

/**
 * Convenience wrapper that holds the toolbar and its preferred mounting position.
 *
 * <pre>{@code
 * var contribution = new GitHubToolbarContribution(ctx, creds, dm.themeProperty(), TOP);
 * BorderPane wrapper = new BorderPane();
 * wrapper.setCenter(dm.getRootPane());
 * switch (contribution.position()) {
 *     case TOP    -> wrapper.setTop(contribution.toolbarNode());
 *     case BOTTOM -> wrapper.setBottom(contribution.toolbarNode());
 * }
 * }</pre>
 */
public class GitHubToolbarContribution {

    public enum Position { TOP, BOTTOM }

    private final GitHubToolbar toolbar;
    private final Position position;

    public GitHubToolbarContribution(GitHubRepoContext ctx,
                                     CredentialStore creds,
                                     ObjectProperty<Theme> theme) {
        this(ctx, creds, theme, Position.TOP);
    }

    public GitHubToolbarContribution(GitHubRepoContext ctx,
                                     CredentialStore creds,
                                     ObjectProperty<Theme> theme,
                                     Position position) {
        this.toolbar  = new GitHubToolbar(ctx, creds, theme);
        this.position = position;
    }

    public Node toolbarNode() { return toolbar; }
    public Position position() { return position; }
    public void dispose()      { toolbar.dispose(); }
}
```

---

## 12. Dialogs (all programmatic, no FXML)

Each dialog uses `Dialog<T>` and returns `Optional<T>` so callers react only on
user confirmation.

### 12.1 `CommitDialog`

```java
package org.metalib.papifly.fx.github.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.Optional;

public final class CommitDialog {
    private CommitDialog() {}

    public static Optional<String> show() {
        Dialog<String> d = new Dialog<>();
        d.setTitle("Commit Changes");
        d.setHeaderText("Enter a commit message");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextArea msg = new TextArea();
        msg.setPromptText("Summary of changes…");
        msg.setPrefRowCount(4);
        msg.setWrapText(true);

        CheckBox stageAll = new CheckBox("Stage all changes");
        stageAll.setSelected(true);

        d.getDialogPane().setContent(new VBox(8, msg, stageAll) {{ setPadding(new Insets(8)); }});
        d.getDialogPane().lookupButton(ButtonType.OK)
                .disableProperty().bind(msg.textProperty().isEmpty());
        d.setResultConverter(btn -> btn == ButtonType.OK ? msg.getText().strip() : null);
        return d.showAndWait();
    }
}
```

### 12.2 `NewBranchDialog`

```java
package org.metalib.papifly.fx.github.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import java.util.Optional;

public final class NewBranchDialog {

    public record Result(String name, String startPoint) {}
    private NewBranchDialog() {}

    public static Optional<Result> show(String currentBranch) {
        Dialog<Result> d = new Dialog<>();
        d.setTitle("New Branch");
        d.setHeaderText("Create and checkout a new branch");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField  = new TextField();
        nameField.setPromptText("feature/my-feature");
        TextField startField = new TextField(currentBranch);

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8); grid.setPadding(new Insets(8));
        grid.addRow(0, new Label("Branch name:"), nameField);
        grid.addRow(1, new Label("Start point:"),  startField);
        d.getDialogPane().setContent(grid);

        d.getDialogPane().lookupButton(ButtonType.OK)
                .disableProperty().bind(nameField.textProperty().isEmpty());
        d.setResultConverter(btn -> btn == ButtonType.OK
                ? new Result(nameField.getText().strip(), startField.getText().strip())
                : null);
        return d.showAndWait();
    }
}
```

### 12.3 `RollbackDialog`

```java
package org.metalib.papifly.fx.github.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.github.model.CommitInfo;
import org.metalib.papifly.fx.github.model.RollbackMode;
import java.util.Optional;

public final class RollbackDialog {
    private RollbackDialog() {}

    public static Optional<RollbackMode> show(CommitInfo head) {
        Dialog<RollbackMode> d = new Dialog<>();
        d.setTitle("Rollback Last Commit");
        d.setHeaderText("Undo commit " + head.shortHash());
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Label info = new Label(
            "Hash: %s\nAuthor: %s\nMessage: %s".formatted(
                head.shortHash(), head.author(), head.message()));

        ToggleGroup group = new ToggleGroup();
        RadioButton revertRb = radio("Revert (safe — new undo commit, works for pushed commits)",
                RollbackMode.REVERT, group);
        RadioButton softRb   = radio("Reset soft (un-commit, keep changes staged — local only)",
                RollbackMode.RESET_SOFT, group);
        RadioButton hardRb   = radio("Reset hard (discard all changes — DESTRUCTIVE)",
                RollbackMode.RESET_HARD, group);

        if (head.isPushed()) {
            revertRb.setSelected(true);
            softRb.setDisable(true);
            hardRb.setDisable(true);
            info.setText(info.getText() + "\n\n⚠ Commit is already pushed. Only Revert is allowed.");
        } else {
            softRb.setSelected(true);
        }

        VBox content = new VBox(8, info, revertRb, softRb, hardRb);
        content.setPadding(new Insets(8));
        d.getDialogPane().setContent(content);

        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Toggle sel = group.getSelectedToggle();
            return sel != null ? (RollbackMode) sel.getUserData() : null;
        });
        return d.showAndWait();
    }

    private static RadioButton radio(String label, RollbackMode mode, ToggleGroup group) {
        RadioButton rb = new RadioButton(label);
        rb.setToggleGroup(group);
        rb.setUserData(mode);
        return rb;
    }
}
```

### 12.4 `PullRequestDialog`

```java
package org.metalib.papifly.fx.github.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.metalib.papifly.fx.github.model.PullRequestDraft;
import java.util.Optional;

public final class PullRequestDialog {
    private PullRequestDialog() {}

    public static Optional<PullRequestDraft> show(String headBranch, String baseBranch) {
        Dialog<PullRequestDraft> d = new Dialog<>();
        d.setTitle("Create Pull Request");
        d.setHeaderText(headBranch + " → " + baseBranch);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField titleField = new TextField();
        titleField.setPromptText("PR title (required)");

        TextArea bodyArea = new TextArea();
        bodyArea.setPromptText("Description (optional)");
        bodyArea.setPrefRowCount(5);
        bodyArea.setWrapText(true);

        CheckBox openBrowser = new CheckBox("Open in browser after creation");
        openBrowser.setSelected(true);

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(6); grid.setPadding(new Insets(8));
        grid.addRow(0, new Label("Head:"), new Label(headBranch));
        grid.addRow(1, new Label("Base:"), new Label(baseBranch));
        grid.addRow(2, new Label("Title:"), titleField);
        GridPane.setHgrow(titleField, Priority.ALWAYS);

        VBox content = new VBox(8, grid, bodyArea, openBrowser);
        content.setPadding(new Insets(0, 8, 8, 8));
        d.getDialogPane().setContent(content);

        d.getDialogPane().lookupButton(ButtonType.OK)
                .disableProperty().bind(titleField.textProperty().isEmpty());

        d.setResultConverter(btn -> btn == ButtonType.OK
                ? new PullRequestDraft(
                        titleField.getText().strip(),
                        bodyArea.getText() == null ? "" : bodyArea.getText().strip(),
                        headBranch,
                        baseBranch,
                        openBrowser.isSelected())
                : null);
        return d.showAndWait();
    }
}
```

### 12.5 `TokenDialog`

```java
package org.metalib.papifly.fx.github.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.Optional;

/**
 * Simple dialog for entering a GitHub Personal Access Token.
 * The token is shown as a password field.
 */
public final class TokenDialog {
    private TokenDialog() {}

    public static Optional<String> show() {
        Dialog<String> d = new Dialog<>();
        d.setTitle("GitHub Token");
        d.setHeaderText("Enter your GitHub Personal Access Token (PAT)");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        PasswordField tokenField = new PasswordField();
        tokenField.setPromptText("ghp_…");

        Label hint = new Label(
            "Requires scopes: repo (for private repos) or public_repo (for public repos).\n" +
            "Generate at: github.com → Settings → Developer settings → PATs.");
        hint.setWrapText(true);
        hint.setStyle("-fx-font-size: 10;");

        VBox content = new VBox(8, tokenField, hint);
        content.setPadding(new Insets(8));
        d.getDialogPane().setContent(content);

        d.getDialogPane().lookupButton(ButtonType.OK)
                .disableProperty().bind(tokenField.textProperty().isEmpty());
        d.setResultConverter(btn -> btn == ButtonType.OK ? tokenField.getText().strip() : null);
        return d.showAndWait();
    }
}
```

### 12.6 `DirtyCheckoutAlert`

```java
package org.metalib.papifly.fx.github.ui.dialog;

import javafx.scene.control.*;
import java.util.Optional;

/**
 * Alerts the user that the working tree is dirty before a branch checkout.
 * Returns {@code Optional.of(true)} when the user chooses to force checkout
 * (discarding local changes), or {@code Optional.empty()} on cancel.
 */
public final class DirtyCheckoutAlert {
    private DirtyCheckoutAlert() {}

    public static Optional<Boolean> show(String targetBranch) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Uncommitted Changes");
        alert.setHeaderText("Working tree has uncommitted changes");
        alert.setContentText(
            "Switching to '" + targetBranch + "' may overwrite your local changes.\n\n" +
            "Commit or stash your changes first, or discard them to force checkout.");

        ButtonType discard = new ButtonType("Discard changes", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel  = new ButtonType("Cancel",          ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(discard, cancel);

        return alert.showAndWait().map(btn -> btn == discard ? Boolean.TRUE : null)
                    .filter(v -> v != null);
    }
}
```

---

## 13. Host Application Integration

### 13.1 Mounting in a host `BorderPane`

```java
// Create DockManager as usual
DockManager dm = new DockManager();
dm.setOwnerStage(primaryStage);

// Build your dock layout...
var group = dm.createTabGroup();
group.addLeaf(dm.createLeaf("Editor", new Label("content")));
dm.setRoot((DockElement) group);

// Construct toolbar
var ctx     = GitHubRepoContext.of(
        URI.create("https://github.com/org-metalib/papiflyfx-docking"),
        Path.of(".")                    // local clone; pass null for remote-only mode
);
var creds   = new PreferencesTokenStore();   // persists PAT across restarts
// Or bootstrap with env var:  new PatCredentialStore(System.getenv("GITHUB_TOKEN"))

var toolbar = new GitHubToolbarContribution(ctx, creds, dm.themeProperty(),
                                             GitHubToolbarContribution.Position.TOP);

// Mount
BorderPane root = new BorderPane();
root.setTop(toolbar.toolbarNode());
root.setCenter(dm.getRootPane());

primaryStage.setScene(new Scene(root, 1200, 800));
primaryStage.setOnHiding(_ -> {
    toolbar.dispose();
    dm.dispose();
});
primaryStage.show();
```

### 13.2 Remote-only mode (no local clone)

When the host app does not have a local clone path, local Git buttons are
automatically disabled by the binding `vm.localDisabled`. The repo link and PR
creation (with a valid token) still work.

```java
var ctx = GitHubRepoContext.remoteOnly(
        URI.create("https://github.com/org-metalib/papiflyfx-docking"));
var toolbar = new GitHubToolbarContribution(ctx, creds, dm.themeProperty());
// Checkout, New Branch, Commit, Rollback, Push are all disabled automatically
```

---

## 14. UX Behaviour Constraints

### 14.1 Commit button enable/disable logic

```
commitBtn.disabled ← onDefaultBranch OR (dirty == false) OR busy
```

**Default branch detection priority:**

1. `GET https://api.github.com/repos/{owner}/{repo}` → `default_branch` field
2. JGit `ls-remote` → symbolic target of `HEAD`
3. Local branch named `main` if present
4. Local branch named `master` if present
5. Fall back to current branch

### 14.2 Rollback safety matrix

| HEAD state         | Pre-selected mode | Available modes         |
|--------------------|-------------------|-------------------------|
| Not yet pushed     | RESET_SOFT        | REVERT, RESET_SOFT, RESET_HARD |
| Already pushed     | REVERT (only)     | REVERT (reset modes disabled) |

### 14.3 Dirty-tree checkout guard

Before switching branches, the ViewModel checks `RepoStatus.isDirty()`. If true:

- `DirtyCheckoutAlert.show(targetBranch)` is displayed
- User chooses "Discard changes" (force=true) or "Cancel"
- "Stash" support is a Phase 6 enhancement

### 14.4 Push authentication guard

Push and Create PR buttons are bound to `vm.pushDisabled` / `vm.prDisabled`.
Both include `authenticated.not()`. When not authenticated, clicking the 🔑 button
opens `TokenDialog` to enter a PAT.

---

## 15. Error Handling

### 15.1 Error categories and messages

| Error                    | Source                      | User-facing message                                      |
|--------------------------|-----------------------------|----------------------------------------------------------|
| Repository not found     | `JGitRepository` constructor | "Cannot open repository at {path}"                       |
| Detached HEAD            | `loadStatus()`              | "HEAD is detached — checkout a branch first"             |
| Dirty working tree       | checkout guard              | Alert dialog before proceeding                           |
| Commit on default branch | ViewModel binding           | Commit button stays disabled (tooltip explains reason)   |
| Auth failure (401/403)   | `GitHubApiService`          | "Authentication failed — check your token"               |
| Rate limited             | `GitHubApiService`          | "GitHub rate limit reached — try again later"            |
| Push rejected (non-ff)   | `JGitRepository.push()`     | "Push rejected: non-fast-forward. Pull first."           |
| Network unreachable      | `HttpClient`                | "Cannot reach GitHub — check network"                    |

### 15.2 Error flow

1. Every command clears `errorText` at start.
2. `CommandRunner.exceptionally` routes any exception to `errorText` on the FX thread.
3. Red `errorLabel` in the toolbar right section displays the current error.
4. User retries by clicking the same button; `errorText` clears again.

### 15.3 Auth error recovery

When a 401/403 comes back from GitHub API, the ViewModel clears the cached token
state. The toolbar shows the error message and keeps the 🔑 button enabled so the
user can re-enter the token immediately.

```java
// In GitHubApiService.send():
if (code == 401 || code == 403) {
    throw new GitHubApiException("Authentication failed (HTTP " + code + ")");
}
// In ViewModel execute() onError handler (optional enhancement):
if (ex instanceof GitHubApiException && ex.getMessage().contains("Authentication failed")) {
    authenticated.set(false);
}
```

---

## 16. Testing Strategy

### 16.1 Unit tests — non-UI

**`RemoteUrlParserTest`** — covers all URL forms:

```java
@ParameterizedTest
@CsvSource({
    "https://github.com/owner/repo.git,      owner, repo",
    "git@github.com:owner/repo.git,          owner, repo",
    "ssh://git@github.com/owner/repo.git,    owner, repo",
    "https://github.com/owner/repo,          owner, repo"
})
void parse_returnsOwnerAndRepo(String remote, String owner, String repo) {
    var coords = RemoteUrlParser.parse(remote);
    assertEquals(owner, coords.owner());
    assertEquals(repo,  coords.repo());
}
```

**`JGitRepositoryTest`** — uses `@TempDir` for isolated repos:

```java
@Test
void createAndCheckout_switchesToNewBranch(@TempDir Path dir) throws Exception {
    Git git = Git.init().setDirectory(dir.toFile()).call();
    Files.writeString(dir.resolve("README.md"), "init");
    git.add().addFilepattern(".").call();
    git.commit().setMessage("initial").call();
    git.close();

    JGitRepository repo = new JGitRepository(dir, null);
    repo.createAndCheckout("feature/x", "HEAD");

    assertEquals("feature/x", repo.loadStatus().currentBranch());
    repo.close();
}

@Test
void rollback_revert_createsNewCommit(@TempDir Path dir) throws Exception {
    // init repo with 2 commits, verify revert adds a 3rd commit
    ...
}

@Test
void rollback_resetSoft_keepsChangesStaged(@TempDir Path dir) throws Exception {
    ...
    repo.rollback(RollbackMode.RESET_SOFT);
    assertTrue(repo.loadStatus().isDirty());
}

@Test
void rollback_resetHard_discardsFile(@TempDir Path dir) throws Exception {
    ...
    repo.rollback(RollbackMode.RESET_HARD);
    assertFalse(Files.exists(dir.resolve("file.txt")));
}
```

**`GitHubApiServiceTest`** — mocks HTTP responses:

```java
@Test
void extractString_parsesDefaultBranch() {
    String json = "{\"default_branch\":\"main\",\"other\":\"value\"}";
    assertEquals("main", GitHubApiService.extractString(json, "default_branch"));
}

@Test
void extractInt_parsesPrNumber() {
    String json = "{\"number\":42,\"html_url\":\"https://github.com/...\"}";
    assertEquals(42, GitHubApiService.extractInt(json, "number"));
}
```

### 16.2 ViewModel unit tests (no FX toolkit)

```java
// Mock GitRepository and verify state transitions
@Test
void commit_setsStatusText_andCallsRepository() {
    var mockRepo = mock(GitRepository.class);
    when(mockRepo.commitAll(any())).thenReturn(
        new CommitInfo("abc123full", "abc123", "msg", "Author",
                       Instant.now(), false));
    // ... set up vm with mockRepo, call vm.commit("msg")
    // ... verify statusText contains "abc123"
}
```

### 16.3 TestFX UI tests (headless)

```java
@ExtendWith(ApplicationExtension.class)
class GitHubToolbarFxTest {

    private GitHubToolbar toolbar;

    @Start
    void start(Stage stage) {
        var ctx   = GitHubRepoContext.remoteOnly(
                URI.create("https://github.com/org-metalib/papiflyfx-docking"));
        var creds = new PatCredentialStore();
        var theme = new SimpleObjectProperty<>(Theme.dark());
        toolbar   = new GitHubToolbar(ctx, creds, theme);
        stage.setScene(new Scene(toolbar, 900, 40));
        stage.show();
    }

    @Test
    void repoLink_displayOwnerSlashRepo(FxRobot robot) {
        assertThat(robot.lookup("org-metalib/papiflyfx-docking").queryAs(Hyperlink.class))
                .isNotNull();
    }

    @Test
    void commitButton_isDisabledWithNoLocalClone(FxRobot robot) {
        assertThat(robot.lookup("Commit…").queryButton()).isDisabled();
    }

    @Test
    void pushButton_isDisabledWhenNotAuthenticated(FxRobot robot) {
        assertThat(robot.lookup("Push").queryButton()).isDisabled();
    }

    @Test
    void tokenButton_isAlwaysEnabled(FxRobot robot) {
        assertThat(robot.lookup("🔑").queryButton()).isEnabled();
    }
}
```

### 16.4 Running tests

```bash
# Full module test run (headless)
./mvnw -pl papiflyfx-docking-github -am -Dtestfx.headless=true test

# Single class
./mvnw -Dtest=JGitRepositoryTest -pl papiflyfx-docking-github -am test

# UI tests
./mvnw -Dtest=GitHubToolbarFxTest -pl papiflyfx-docking-github -am \
       -Dtestfx.headless=true test
```

---

## 17. Implementation Phases

### Phase 1 — Scaffold & read-only status

- Add module folder and `pom.xml`; register in root aggregator
- Implement `GitHubRepoContext`, `BranchRef`, `RepoStatus`, `CommitInfo`
- Implement `RemoteUrlParser`
- Implement `JGitRepository` (read-only: `loadStatus`, `listBranches`, `getHeadCommit`)
- Implement `PatCredentialStore`, `PreferencesTokenStore`
- Build `GitHubToolbar` shell: repo link, branch label + dirty dot, branch combo, spinner
- Theme binding from `dm.themeProperty()`
- Tests: `RemoteUrlParserTest`, basic `JGitRepositoryTest`, `GitHubToolbarFxTest` (render only)

### Phase 2 — Branch operations

- Implement `checkout`, `createAndCheckout` in `JGitRepository`
- Build `NewBranchDialog`
- Build `DirtyCheckoutAlert`
- Wire Checkout and New Branch… buttons in toolbar
- Tests: branch operation unit tests; UI test for button enable/disable

### Phase 3 — Commit & rollback

- Implement `commitAll`, `getHeadCommit`, `rollback` in `JGitRepository`
- Implement `RollbackMode`
- Build `CommitDialog`, `RollbackDialog`
- Implement `detectDefaultBranch`
- Wire commit-disabled binding and rollback button
- Tests: commit, rollback (all three modes) unit tests

### Phase 4 — Push & authentication

- Implement `CredentialStore` hierarchy and `TokenDialog`
- Implement `push` in `JGitRepository` with push-result validation
- Wire Push button with auth guard and 🔑 token entry flow
- Test push against a local bare remote (created in `@TempDir`)

### Phase 5 — PR creation (GitHub REST)

- Implement `GitHubApiService` (default branch fetch + PR creation)
- Implement `GitHubApiException`
- Build `PullRequestDialog`
- Wire Create PR button; open browser on success
- Tests: JSON extraction unit tests; optional live integration test via env vars

### Phase 6 — Hardening & polish

- Retry / exponential backoff for transient GitHub API errors (rate limit, 5xx)
- Stash-then-checkout option in `DirtyCheckoutAlert`
- Protected-branch detection from GitHub API; improve disabled-commit tooltip
- Optional OS keychain for token storage (replace `PreferencesTokenStore`)
- Optional OAuth device-flow auth path
- Contribution to `papiflyfx-docking-samples` demo app
- Documentation: update `spec/papiflyfx-docking-github/README.md` and module README

---

## 18. Key Design Decisions

| Decision | Rationale |
|---|---|
| **JGit over shelling out** | Cross-platform, structured exceptions, no external `git` binary dependency |
| **`java.net.http` over `github-api` library** | Zero extra runtime dependencies; matches project convention of hand-rolled JSON |
| **`CommandRunner` with `CompletableFuture`** | Cleanly separates threading concern; easy to test with mock suppliers |
| **PAT-first auth** | Fastest path; OAuth device-flow deferred to Phase 6 |
| **`PreferencesTokenStore`** | Persists PAT across restarts using standard JDK API; swappable for keychain |
| **HBox toolbar, not `DockLeaf`** | Fixed app-chrome UI; `BorderPane.setTop/setBottom` is the natural mount point |
| **`api` compile scope, `docks` test scope only** | Minimises coupling; matches `papiflyfx-docking-code` pattern |
| **Revert-only for pushed commits** | Prevents history rewrites on shared branches; UX-safe default |
| **Sealed `GitRepository` interface** | Enables mock injection in ViewModel unit tests without a live filesystem |
| **`GitHubRepoContext` parses owner/repo at construction** | Ensures coordinates are always consistent; surfaces bad URLs early |

# papiflyfx-docking-github — Implementation Plan

> Synthesised from research by ChatGPT, Gemini, and Grok; refined to align with the
> existing PapiflyFX docking architecture (v 0.0.14-SNAPSHOT).

---

## 1 Module Identity

| Field | Value |
|---|---|
| artifactId | `papiflyfx-docking-github` |
| groupId | `org.metalib.papifly.docking` |
| Java package | `org.metalib.papifly.fx.github` |
| Sub-packages | `.service`, `.model`, `.ui`, `.ui.dialog`, `.auth` |

**Dependency flow:**

```
papiflyfx-docking-api  ←  papiflyfx-docking-github  (compile)
papiflyfx-docking-docks ←  papiflyfx-docking-github  (test only)
JGit                    ←  papiflyfx-docking-github  (compile)
javafx-controls         ←  papiflyfx-docking-github  (compile, transitive from api)
```

No external JSON library — the project convention is `java.util.Map`-based JSON
via `DockSessionSerializer`. The module follows the same pattern for any
serialisation needs.

---

## 2 Maven POM

Register the module in the root aggregator and create the child POM. JGit version
is managed through a property in the parent POM to keep all dependency versions
centralised.

### 2.1 Root `pom.xml` changes

```xml
<!-- add to <modules> -->
<module>papiflyfx-docking-github</module>

<!-- add to <properties> -->
<jgit.version>7.2.0.202503040940-r</jgit.version>

<!-- add to <dependencyManagement> -->
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
            <version>${javafx.version}</version>
            <classifier>${javafx.platform}</classifier>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-graphics</artifactId>
            <version>${javafx.version}</version>
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

        <!-- Docks (test scope — for integration / UI tests) -->
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

## 3 Architecture Overview

The module follows a three-layer architecture mirroring the patterns established in
`papiflyfx-docking-code` (controller/model/view separation with background
`Task` for I/O).

```
┌──────────────────────────────────────────────────────────┐
│                      UI Layer                            │
│  GitHubToolbar ← binds → GitHubToolbarViewModel          │
│  Dialogs: CommitDialog, NewBranchDialog, PullRequestDialog│
└────────────────┬──────────────────┬──────────────────────┘
                 │                  │
    ┌────────────▼────┐    ┌───────▼─────────────┐
    │  Git Service     │    │  GitHub API Service  │
    │  (JGit)          │    │  (java.net.http)     │
    │                  │    │                      │
    │  branch, commit, │    │  default branch,     │
    │  reset, revert,  │    │  create PR,          │
    │  push, status    │    │  repo metadata       │
    └────────────────  ┘    └──────────────────────┘
```

### 3.1 Package layout

```
org.metalib.papifly.fx.github
├── GitHubToolbar.java              // top-level public Node
├── GitHubToolbarContribution.java  // DockingToolbarContribution impl
├── GitHubRepoContext.java          // config record
│
├── model/
│   ├── BranchInfo.java
│   ├── CommitInfo.java
│   ├── RepoStatus.java
│   └── RollbackMode.java
│
├── service/
│   ├── GitService.java             // local JGit operations
│   └── GitHubApiService.java       // remote REST operations
│
├── auth/
│   ├── CredentialStore.java        // PAT storage abstraction
│   └── PatCredentialStore.java     // simple PAT impl
│
├── ui/
│   ├── GitHubToolbarViewModel.java // observable state
│   ├── ToolbarRenderer.java        // builds JavaFX nodes
│   └── StatusIndicator.java        // dirty/clean badge
│
└── ui/dialog/
    ├── CommitDialog.java
    ├── NewBranchDialog.java
    ├── PullRequestDialog.java
    └── RollbackDialog.java
```

---

## 4 Model Layer

### 4.1 `GitHubRepoContext`

Configuration record supplied by the host application. The `localClonePath` is
optional — when absent the toolbar operates in "remote-only" mode (repo link and
PR creation, but no branch/commit/push).

```java
package org.metalib.papifly.fx.github;

import java.net.URI;
import java.nio.file.Path;

/**
 * Immutable configuration identifying the target GitHub repository.
 *
 * @param remoteUrl      HTTPS or SSH URL of the remote repository (required)
 * @param localClonePath local working-copy root (nullable; null → remote-only mode)
 * @param owner          GitHub owner extracted from remoteUrl
 * @param repo           GitHub repo name extracted from remoteUrl
 */
public record GitHubRepoContext(
        URI remoteUrl,
        Path localClonePath,
        String owner,
        String repo
) {
    /** Convenience factory that parses owner/repo from a standard GitHub URL. */
    public static GitHubRepoContext of(URI remoteUrl, Path localClonePath) {
        var parsed = parseOwnerRepo(remoteUrl);
        return new GitHubRepoContext(remoteUrl, localClonePath, parsed[0], parsed[1]);
    }

    public static GitHubRepoContext remoteOnly(URI remoteUrl) {
        return of(remoteUrl, null);
    }

    public boolean hasLocalClone() {
        return localClonePath != null;
    }

    private static String[] parseOwnerRepo(URI uri) {
        // handles https://github.com/owner/repo.git  AND  git@github.com:owner/repo.git
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            // SSH style: git@github.com:owner/repo.git
            String ssp = uri.getSchemeSpecificPart();
            int colon = ssp.indexOf(':');
            path = ssp.substring(colon + 1);
        }
        path = path.replaceFirst("^/", "").replaceFirst("\\.git$", "");
        String[] parts = path.split("/", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Cannot parse owner/repo from: " + uri);
        }
        return parts;
    }
}
```

### 4.2 `BranchInfo`

```java
package org.metalib.papifly.fx.github.model;

/**
 * A lightweight snapshot of a Git branch.
 *
 * @param name       short ref name (e.g. "feature/login")
 * @param isRemote   true when this ref is a remote-tracking branch
 * @param isCurrent  true when this is the currently checked-out branch
 */
public record BranchInfo(String name, boolean isRemote, boolean isCurrent) {}
```

### 4.3 `RepoStatus`

```java
package org.metalib.papifly.fx.github.model;

import java.util.Set;

/**
 * Working-tree status of a local Git clone.
 */
public record RepoStatus(
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

    public int changedFileCount() {
        return added.size() + changed.size() + removed.size();
    }
}
```

### 4.4 `CommitInfo`

```java
package org.metalib.papifly.fx.github.model;

import java.time.Instant;

/**
 * Metadata for a single Git commit.
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

### 4.5 `RollbackMode`

```java
package org.metalib.papifly.fx.github.model;

/**
 * Strategy for undoing the last commit.
 *
 * <ul>
 *   <li>{@code RESET_SOFT} — un-commit but keep changes staged (local only, rewrites history)</li>
 *   <li>{@code RESET_HARD} — un-commit and discard changes (local only, destructive)</li>
 *   <li>{@code REVERT} — create a new commit that undoes the last (safe for pushed commits)</li>
 * </ul>
 */
public enum RollbackMode {
    RESET_SOFT,
    RESET_HARD,
    REVERT
}
```

---

## 5 Service Layer

### 5.1 `GitService` — local JGit operations

All methods run off the FX application thread. The caller (ViewModel) wraps each
call in a `javafx.concurrent.Task`.

```java
package org.metalib.papifly.fx.github.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.metalib.papifly.fx.github.model.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public class GitService implements AutoCloseable {

    private final Git git;
    private final CredentialsProvider credentials;

    public GitService(Path workingDir, CredentialsProvider credentials) throws IOException {
        this.git = Git.open(workingDir.toFile());
        this.credentials = credentials;
    }

    // -- Branch operations ---------------------------------------------------

    /** Current checked-out branch name; null if detached HEAD. */
    public String getCurrentBranch() throws IOException {
        return git.getRepository().getBranch();
    }

    /** All local and remote branches. */
    public List<BranchInfo> listBranches() throws GitAPIException, IOException {
        String current = getCurrentBranch();
        var result = new ArrayList<BranchInfo>();

        for (Ref ref : git.branchList().call()) {
            String name = Repository.shortenRefName(ref.getName());
            result.add(new BranchInfo(name, false, name.equals(current)));
        }
        for (Ref ref : git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call()) {
            String name = Repository.shortenRefName(ref.getName());
            result.add(new BranchInfo(name, true, false));
        }
        return Collections.unmodifiableList(result);
    }

    /** Checkout an existing branch. */
    public void checkoutBranch(String name) throws GitAPIException {
        git.checkout().setName(name).call();
    }

    /** Create and checkout a new branch from startPoint (branch name, tag, or commit). */
    public void createAndCheckoutBranch(String name, String startPoint) throws GitAPIException {
        git.checkout()
           .setCreateBranch(true)
           .setName(name)
           .setStartPoint(startPoint)
           .call();
    }

    // -- Status & commit -----------------------------------------------------

    /** Working-tree status snapshot. */
    public RepoStatus getStatus() throws GitAPIException {
        Status s = git.status().call();
        return new RepoStatus(
                s.getAdded(),
                s.getChanged(),
                s.getRemoved(),
                s.getUntracked(),
                s.getConflicting()
        );
    }

    /** Stage all changes and commit with the given message. */
    public CommitInfo commit(String message) throws GitAPIException {
        git.add().addFilepattern(".").call();
        // also stage deletions
        git.add().addFilepattern(".").setUpdate(true).call();
        RevCommit rev = git.commit().setMessage(message).call();
        return toCommitInfo(rev, false);
    }

    /** Head commit metadata (for rollback dialog preview). */
    public CommitInfo getHeadCommit() throws IOException, GitAPIException {
        RevCommit head = git.log().setMaxCount(1).call().iterator().next();
        boolean pushed = isHeadPushed();
        return toCommitInfo(head, pushed);
    }

    // -- Rollback ------------------------------------------------------------

    /** Undo the last commit according to the chosen mode. */
    public void rollbackLastCommit(RollbackMode mode) throws GitAPIException {
        switch (mode) {
            case RESET_SOFT -> git.reset()
                    .setMode(ResetCommand.ResetType.SOFT)
                    .setRef("HEAD~1")
                    .call();
            case RESET_HARD -> git.reset()
                    .setMode(ResetCommand.ResetType.HARD)
                    .setRef("HEAD~1")
                    .call();
            case REVERT     -> git.revert()
                    .include(git.getRepository().resolve("HEAD"))
                    .call();
        }
    }

    // -- Push ----------------------------------------------------------------

    /** Push current branch to origin. */
    public void push() throws GitAPIException {
        git.push()
           .setCredentialsProvider(credentials)
           .call();
    }

    // -- Helpers -------------------------------------------------------------

    /** Best-effort check: is HEAD pushed to origin? */
    public boolean isHeadPushed() throws IOException {
        String branch = getCurrentBranch();
        if (branch == null) return false;
        BranchTrackingStatus tracking = BranchTrackingStatus.of(git.getRepository(), branch);
        return tracking != null && tracking.getAheadCount() == 0;
    }

    /** Determine the default branch (main, master, or remote HEAD). */
    public String detectDefaultBranch() throws IOException, GitAPIException {
        // check remote HEAD first
        var remoteRefs = git.lsRemote()
                .setCredentialsProvider(credentials)
                .callAsMap();
        Ref head = remoteRefs.get("HEAD");
        if (head != null && head.getTarget() != null) {
            return Repository.shortenRefName(head.getTarget().getName());
        }
        // fallback heuristic
        Set<String> locals = new HashSet<>();
        for (Ref ref : git.branchList().call()) {
            locals.add(Repository.shortenRefName(ref.getName()));
        }
        if (locals.contains("main")) return "main";
        if (locals.contains("master")) return "master";
        return getCurrentBranch();
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

### 5.2 `GitHubApiService` — remote REST operations

Uses **`java.net.http.HttpClient`** (no external JSON library) following the
project convention of zero extra dependencies for serialisation.

```java
package org.metalib.papifly.fx.github.service;

import org.metalib.papifly.fx.github.auth.CredentialStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thin wrapper around the GitHub REST API.
 * Uses only {@code java.net.http} and hand-rolled JSON to stay dependency-free.
 */
public class GitHubApiService {

    private static final String API_BASE = "https://api.github.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;
    private final CredentialStore credentialStore;

    public GitHubApiService(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    // -- Repository metadata -------------------------------------------------

    /** Fetch the default branch of a GitHub repository. */
    public String getDefaultBranch(String owner, String repo)
            throws IOException, InterruptedException {
        String body = get("/repos/%s/%s".formatted(owner, repo));
        return extractJsonString(body, "default_branch");
    }

    // -- Pull Request --------------------------------------------------------

    /**
     * Create a pull request.
     *
     * @return the HTML URL of the newly created PR
     */
    public String createPullRequest(String owner, String repo,
                                    String title, String head, String base,
                                    String body)
            throws IOException, InterruptedException {
        String jsonBody = """
                {
                  "title": "%s",
                  "head":  "%s",
                  "base":  "%s",
                  "body":  "%s"
                }
                """.formatted(
                escapeJson(title),
                escapeJson(head),
                escapeJson(base),
                escapeJson(body != null ? body : "")
        );
        String response = post("/repos/%s/%s/pulls".formatted(owner, repo), jsonBody);
        return extractJsonString(response, "html_url");
    }

    // -- HTTP helpers --------------------------------------------------------

    private String get(String path) throws IOException, InterruptedException {
        HttpRequest request = newRequest(path)
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp);
        return resp.body();
    }

    private String post(String path, String jsonBody) throws IOException, InterruptedException {
        HttpRequest request = newRequest(path)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp);
        return resp.body();
    }

    private HttpRequest.Builder newRequest(String path) {
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + path))
                .timeout(TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28");

        credentialStore.getToken().ifPresent(token ->
                builder.header("Authorization", "Bearer " + token));

        return builder;
    }

    private void checkStatus(HttpResponse<String> resp) throws IOException {
        int code = resp.statusCode();
        if (code < 200 || code >= 300) {
            throw new IOException("GitHub API %d: %s".formatted(code, resp.body()));
        }
    }

    /** Minimal JSON string extractor — avoids adding a JSON library. */
    static String extractJsonString(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
```

---

## 6 Authentication Layer

### 6.1 `CredentialStore` interface

```java
package org.metalib.papifly.fx.github.auth;

import java.util.Optional;

/**
 * Abstraction for secure token storage.
 * <p>
 * Phase 1 ships with a PAT-based implementation. A future phase
 * may add OAuth device-flow or macOS Keychain integration.
 */
public interface CredentialStore {

    /** Retrieve the current access token, if configured. */
    Optional<String> getToken();

    /** Persist a new token. */
    void setToken(String token);

    /** Delete any stored token. */
    void clearToken();

    /** True when a usable token is present. */
    default boolean isAuthenticated() {
        return getToken().isPresent();
    }
}
```

### 6.2 `PatCredentialStore` — simple in-memory + optional file persistence

```java
package org.metalib.papifly.fx.github.auth;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PAT-first credential store. Holds the token in memory;
 * a future iteration may persist it to an encrypted file or OS keychain.
 */
public class PatCredentialStore implements CredentialStore {

    private final AtomicReference<String> token = new AtomicReference<>();

    public PatCredentialStore() {}

    public PatCredentialStore(String initialToken) {
        this.token.set(initialToken);
    }

    @Override
    public Optional<String> getToken() {
        return Optional.ofNullable(token.get());
    }

    @Override
    public void setToken(String token) {
        this.token.set(token);
    }

    @Override
    public void clearToken() {
        this.token.set(null);
    }
}
```

The PAT is also used for JGit push credentials:

```java
// In GitService constructor or factory
CredentialsProvider jgitCredentials = credentialStore.getToken()
    .map(t -> (CredentialsProvider) new UsernamePasswordCredentialsProvider(t, ""))
    .orElse(null);
```

---

## 7 ViewModel — Observable State for the Toolbar

The ViewModel is the single source of truth for the toolbar UI. All Git and
GitHub API calls are dispatched via `javafx.concurrent.Task` to keep the FX
thread free. Property bindings drive enable/disable logic and status indicators.

```java
package org.metalib.papifly.fx.github.ui;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.metalib.papifly.fx.github.GitHubRepoContext;
import org.metalib.papifly.fx.github.auth.CredentialStore;
import org.metalib.papifly.fx.github.model.*;
import org.metalib.papifly.fx.github.service.GitHubApiService;
import org.metalib.papifly.fx.github.service.GitService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GitHubToolbarViewModel {

    // -- Observable state (bound by UI) --------------------------------------

    private final StringProperty currentBranch = new SimpleStringProperty("");
    private final StringProperty defaultBranch = new SimpleStringProperty("main");
    private final ObservableList<BranchInfo> branches = FXCollections.observableArrayList();
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final StringProperty lastError = new SimpleStringProperty("");
    private final ObjectProperty<CommitInfo> headCommit = new SimpleObjectProperty<>();
    private final BooleanProperty authenticated = new SimpleBooleanProperty(false);
    private final BooleanProperty localCloneAvailable = new SimpleBooleanProperty(false);

    // -- Derived read-only properties ----------------------------------------

    /** True when current branch equals default branch — commit must be disabled. */
    public ReadOnlyBooleanProperty onDefaultBranchProperty() {
        var prop = new SimpleBooleanProperty();
        currentBranch.addListener((_, _, newVal) ->
                prop.set(newVal != null && newVal.equals(defaultBranch.get())));
        defaultBranch.addListener((_, _, newVal) ->
                prop.set(currentBranch.get() != null && currentBranch.get().equals(newVal)));
        return prop;
    }

    // -- Services ------------------------------------------------------------

    private final GitHubRepoContext context;
    private final CredentialStore credentialStore;
    private GitService gitService;
    private GitHubApiService githubApiService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        var t = new Thread(r, "github-toolbar-worker");
        t.setDaemon(true);
        return t;
    });

    public GitHubToolbarViewModel(GitHubRepoContext context, CredentialStore credentialStore) {
        this.context = context;
        this.credentialStore = credentialStore;
        this.githubApiService = new GitHubApiService(credentialStore);
        this.authenticated.set(credentialStore.isAuthenticated());
        this.localCloneAvailable.set(context.hasLocalClone());

        if (context.hasLocalClone()) {
            initLocalServices();
        }
    }

    // -- Commands (called by UI action handlers) -----------------------------

    public void refresh() {
        runInBackground("Refreshing…", () -> {
            if (gitService != null) {
                String branch = gitService.getCurrentBranch();
                List<BranchInfo> branchList = gitService.listBranches();
                RepoStatus status = gitService.getStatus();
                CommitInfo head = gitService.getHeadCommit();
                String defBranch = gitService.detectDefaultBranch();

                Platform.runLater(() -> {
                    currentBranch.set(branch);
                    branches.setAll(branchList);
                    dirty.set(status.isDirty());
                    headCommit.set(head);
                    defaultBranch.set(defBranch);
                });
            }
        });
    }

    public void switchBranch(String branchName) {
        runInBackground("Switching to " + branchName + "…", () -> {
            gitService.checkoutBranch(branchName);
            Platform.runLater(this::refresh);
        });
    }

    public void createBranch(String name, String startPoint) {
        runInBackground("Creating branch " + name + "…", () -> {
            gitService.createAndCheckoutBranch(name, startPoint);
            Platform.runLater(this::refresh);
        });
    }

    public void commit(String message) {
        runInBackground("Committing…", () -> {
            CommitInfo info = gitService.commit(message);
            Platform.runLater(() -> {
                headCommit.set(info);
                statusMessage.set("Committed " + info.shortHash());
                refresh();
            });
        });
    }

    public void rollback(RollbackMode mode) {
        runInBackground("Rolling back…", () -> {
            gitService.rollbackLastCommit(mode);
            Platform.runLater(this::refresh);
        });
    }

    public void push() {
        runInBackground("Pushing…", () -> {
            gitService.push();
            Platform.runLater(() -> {
                statusMessage.set("Pushed successfully");
                refresh();
            });
        });
    }

    public void createPullRequest(String title, String body) {
        runInBackground("Creating PR…", () -> {
            String head = currentBranch.get();
            String base = defaultBranch.get();
            String prUrl = githubApiService.createPullRequest(
                    context.owner(), context.repo(), title, head, base, body);
            Platform.runLater(() -> statusMessage.set("PR created: " + prUrl));
        });
    }

    // -- Property accessors (for binding) ------------------------------------

    public StringProperty currentBranchProperty() { return currentBranch; }
    public StringProperty defaultBranchProperty() { return defaultBranch; }
    public ObservableList<BranchInfo> getBranches() { return branches; }
    public BooleanProperty dirtyProperty() { return dirty; }
    public BooleanProperty busyProperty() { return busy; }
    public StringProperty statusMessageProperty() { return statusMessage; }
    public StringProperty lastErrorProperty() { return lastError; }
    public ObjectProperty<CommitInfo> headCommitProperty() { return headCommit; }
    public BooleanProperty authenticatedProperty() { return authenticated; }
    public BooleanProperty localCloneAvailableProperty() { return localCloneAvailable; }
    public GitHubRepoContext getContext() { return context; }

    // -- Internal ------------------------------------------------------------

    private void initLocalServices() {
        try {
            var jgitCreds = credentialStore.getToken()
                    .map(t -> (org.eclipse.jgit.transport.CredentialsProvider)
                            new UsernamePasswordCredentialsProvider(t, ""))
                    .orElse(null);
            gitService = new GitService(context.localClonePath(), jgitCreds);
        } catch (Exception e) {
            lastError.set("Failed to open repository: " + e.getMessage());
            localCloneAvailable.set(false);
        }
    }

    private void runInBackground(String message, ThrowingRunnable action) {
        var task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                action.run();
                return null;
            }
        };
        task.setOnRunning(_ -> {
            busy.set(true);
            statusMessage.set(message);
            lastError.set("");
        });
        task.setOnSucceeded(_ -> busy.set(false));
        task.setOnFailed(_ -> {
            busy.set(false);
            Throwable ex = task.getException();
            lastError.set(ex != null ? ex.getMessage() : "Unknown error");
        });
        executor.submit(task);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    public void dispose() {
        executor.shutdownNow();
        if (gitService != null) gitService.close();
    }
}
```

---

## 8 UI Layer

### 8.1 `GitHubToolbar` — top-level toolbar `Node`

This is a programmatic JavaFX `HBox` (no FXML, no CSS — matching the project
convention). It binds to the ViewModel properties for live updates.

```java
package org.metalib.papifly.fx.github;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.github.auth.CredentialStore;
import org.metalib.papifly.fx.github.model.BranchInfo;
import org.metalib.papifly.fx.github.ui.GitHubToolbarViewModel;
import org.metalib.papifly.fx.github.ui.dialog.*;

import java.awt.Desktop;
import java.net.URI;

/**
 * A horizontal toolbar providing GitHub/Git operations.
 * <p>
 * Mount via {@code BorderPane.setTop(toolbar)} or {@code setBottom(toolbar)}
 * in the host application.
 */
public class GitHubToolbar extends HBox {

    private final GitHubToolbarViewModel viewModel;
    private final ObjectProperty<Theme> themeProperty;

    // -- UI nodes ------------------------------------------------------------
    private final Hyperlink repoLink = new Hyperlink();
    private final Label branchLabel = new Label();
    private final Circle dirtyIndicator = new Circle(4);
    private final ComboBox<String> branchCombo = new ComboBox<>();
    private final Button switchBtn = new Button("Checkout");
    private final Button newBranchBtn = new Button("New Branch…");
    private final Button commitBtn = new Button("Commit…");
    private final Button rollbackBtn = new Button("Rollback…");
    private final Button pushBtn = new Button("Push");
    private final Button prBtn = new Button("Create PR…");
    private final ProgressIndicator spinner = new ProgressIndicator();
    private final Label statusLabel = new Label();
    private final Label errorLabel = new Label();

    public GitHubToolbar(GitHubRepoContext context,
                         CredentialStore credentialStore,
                         ObjectProperty<Theme> themeProperty) {
        this.themeProperty = themeProperty;
        this.viewModel = new GitHubToolbarViewModel(context, credentialStore);

        buildLayout();
        bindProperties();
        wireActions();
        applyTheme(themeProperty.get());
        themeProperty.addListener((_, _, t) -> applyTheme(t));

        viewModel.refresh();
    }

    // -- Layout --------------------------------------------------------------

    private void buildLayout() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        setPadding(new Insets(4, 8, 4, 8));

        spinner.setMaxSize(16, 16);
        spinner.setVisible(false);
        errorLabel.setTextFill(Color.TOMATO);

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(
                repoLink,
                new Separator(),
                branchLabel, dirtyIndicator, branchCombo, switchBtn, newBranchBtn,
                new Separator(),
                commitBtn, rollbackBtn, pushBtn,
                new Separator(),
                prBtn,
                spacer,
                spinner, statusLabel, errorLabel
        );
    }

    // -- Property bindings ---------------------------------------------------

    private void bindProperties() {
        var ctx = viewModel.getContext();
        repoLink.setText(ctx.owner() + "/" + ctx.repo());

        branchLabel.textProperty().bind(viewModel.currentBranchProperty());

        // dirty indicator colour
        viewModel.dirtyProperty().addListener((_, _, isDirty) ->
                dirtyIndicator.setFill(isDirty ? Color.ORANGE : Color.LIMEGREEN));
        dirtyIndicator.setFill(Color.LIMEGREEN);

        // branch combo
        viewModel.getBranches().addListener(
                (javafx.collections.ListChangeListener<BranchInfo>) _ -> {
            branchCombo.getItems().setAll(
                    viewModel.getBranches().stream()
                            .filter(b -> !b.isRemote())
                            .map(BranchInfo::name)
                            .toList()
            );
        });

        // disable commit on default branch
        commitBtn.disableProperty().bind(
                viewModel.onDefaultBranchProperty()
                        .or(viewModel.dirtyProperty().not())
                        .or(viewModel.busyProperty())
        );

        // disable local-only actions when no clone available
        var noLocal = viewModel.localCloneAvailableProperty().not();
        switchBtn.disableProperty().bind(noLocal.or(viewModel.busyProperty()));
        newBranchBtn.disableProperty().bind(noLocal.or(viewModel.busyProperty()));
        rollbackBtn.disableProperty().bind(noLocal.or(viewModel.busyProperty()));
        pushBtn.disableProperty().bind(noLocal.or(viewModel.busyProperty())
                .or(viewModel.authenticatedProperty().not()));
        prBtn.disableProperty().bind(viewModel.busyProperty()
                .or(viewModel.authenticatedProperty().not()));

        // spinner + status
        spinner.visibleProperty().bind(viewModel.busyProperty());
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
        errorLabel.textProperty().bind(viewModel.lastErrorProperty());
    }

    // -- Action handlers -----------------------------------------------------

    private void wireActions() {
        repoLink.setOnAction(_ -> openInBrowser(
                "https://github.com/" + viewModel.getContext().owner()
                        + "/" + viewModel.getContext().repo()));

        switchBtn.setOnAction(_ -> {
            String selected = branchCombo.getValue();
            if (selected != null) viewModel.switchBranch(selected);
        });

        newBranchBtn.setOnAction(_ ->
                NewBranchDialog.show(viewModel.currentBranchProperty().get())
                        .ifPresent(result -> viewModel.createBranch(result.name(), result.startPoint()))
        );

        commitBtn.setOnAction(_ ->
                CommitDialog.show()
                        .ifPresent(viewModel::commit)
        );

        rollbackBtn.setOnAction(_ -> {
            var head = viewModel.headCommitProperty().get();
            if (head != null) {
                RollbackDialog.show(head)
                        .ifPresent(viewModel::rollback);
            }
        });

        pushBtn.setOnAction(_ -> viewModel.push());

        prBtn.setOnAction(_ ->
                PullRequestDialog.show(
                        viewModel.currentBranchProperty().get(),
                        viewModel.defaultBranchProperty().get()
                ).ifPresent(result ->
                        viewModel.createPullRequest(result.title(), result.body()))
        );
    }

    // -- Theme ---------------------------------------------------------------

    private void applyTheme(Theme theme) {
        setStyle("-fx-background-color: " + toHex(theme.headerBackground()) + ";");
        branchLabel.setTextFill(theme.textColor());
        statusLabel.setTextFill(theme.textColor());
        repoLink.setTextFill(theme.accentColor());
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    // -- Utilities -----------------------------------------------------------

    private static void openInBrowser(String url) {
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception ignore) {
            // fallback: could use HostServices if stage is accessible
        }
    }

    /** Release background resources. */
    public void dispose() {
        viewModel.dispose();
    }
}
```

### 8.2 `GitHubToolbarContribution` — docking integration point

The host application (e.g. `papiflyfx-docking-samples`) uses this class to mount
the toolbar into its `BorderPane` wrapper.

```java
package org.metalib.papifly.fx.github;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.github.auth.CredentialStore;

/**
 * Contribution API for mounting the GitHub toolbar.
 * <p>
 * Usage in a host application:
 * <pre>{@code
 * var contribution = new GitHubToolbarContribution(context, credStore, dm.themeProperty());
 *
 * BorderPane wrapper = new BorderPane();
 * wrapper.setCenter(dm.getRootPane());
 *
 * switch (contribution.preferredPosition()) {
 *     case TOP    -> wrapper.setTop(contribution.toolbarNode());
 *     case BOTTOM -> wrapper.setBottom(contribution.toolbarNode());
 * }
 * }</pre>
 */
public class GitHubToolbarContribution {

    public enum DockPosition { TOP, BOTTOM }

    private final GitHubToolbar toolbar;
    private final DockPosition position;

    public GitHubToolbarContribution(GitHubRepoContext context,
                                     CredentialStore credentialStore,
                                     ObjectProperty<Theme> themeProperty) {
        this(context, credentialStore, themeProperty, DockPosition.TOP);
    }

    public GitHubToolbarContribution(GitHubRepoContext context,
                                     CredentialStore credentialStore,
                                     ObjectProperty<Theme> themeProperty,
                                     DockPosition position) {
        this.toolbar = new GitHubToolbar(context, credentialStore, themeProperty);
        this.position = position;
    }

    public Node toolbarNode() {
        return toolbar;
    }

    public DockPosition preferredPosition() {
        return position;
    }

    public void dispose() {
        toolbar.dispose();
    }
}
```

---

## 9 Dialogs

All dialogs are programmatic (no FXML). They return `Optional` results so the
caller can react only when the user confirms.

### 9.1 `CommitDialog`

```java
package org.metalib.papifly.fx.github.ui.dialog;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

import java.util.Optional;

/**
 * Modal dialog for entering a commit message.
 */
public final class CommitDialog {

    private CommitDialog() {}

    /**
     * Show the commit dialog.
     *
     * @return the commit message, or empty if cancelled
     */
    public static Optional<String> show() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Commit Changes");
        dialog.setHeaderText("Enter a commit message");

        var commitType = dialog.getDialogPane().getButtonTypes();
        commitType.addAll(ButtonType.OK, ButtonType.CANCEL);

        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Summary of changes…");
        messageArea.setPrefRowCount(4);
        messageArea.setWrapText(true);

        CheckBox stageAll = new CheckBox("Stage all changes");
        stageAll.setSelected(true);

        var content = new VBox(8, messageArea, stageAll);
        content.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(content);

        // disable OK when message is blank
        dialog.getDialogPane().lookupButton(ButtonType.OK)
                .disableProperty()
                .bind(messageArea.textProperty().isEmpty());

        dialog.setResultConverter(btn ->
                btn == ButtonType.OK ? messageArea.getText().strip() : null);

        return dialog.showAndWait();
    }
}
```

### 9.2 `NewBranchDialog`

```java
package org.metalib.papifly.fx.github.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;

/**
 * Modal dialog for creating a new branch.
 */
public final class NewBranchDialog {

    /** Result record returned when the user confirms. */
    public record Result(String name, String startPoint) {}

    private NewBranchDialog() {}

    /**
     * @param currentBranch pre-filled start point
     * @return the new branch result, or empty if cancelled
     */
    public static Optional<Result> show(String currentBranch) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.setTitle("New Branch");
        dialog.setHeaderText("Create and checkout a new branch");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("feature/my-feature");

        TextField startField = new TextField(currentBranch);
        startField.setPromptText("Start point (branch, tag, or SHA)");

        var grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(8));
        grid.add(new Label("Branch name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Start point:"), 0, 1);
        grid.add(startField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(ButtonType.OK)
                .disableProperty()
                .bind(nameField.textProperty().isEmpty());

        dialog.setResultConverter(btn ->
                btn == ButtonType.OK
                        ? new Result(nameField.getText().strip(), startField.getText().strip())
                        : null);

        return dialog.showAndWait();
    }
}
```

### 9.3 `RollbackDialog`

```java
package org.metalib.papifly.fx.github.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.github.model.CommitInfo;
import org.metalib.papifly.fx.github.model.RollbackMode;

import java.util.Optional;

/**
 * Modal dialog for rolling back the last commit.
 * Recommends {@code REVERT} when the commit is already pushed.
 */
public final class RollbackDialog {

    private RollbackDialog() {}

    /**
     * @param headCommit metadata of the commit to be rolled back
     * @return chosen rollback mode, or empty if cancelled
     */
    public static Optional<RollbackMode> show(CommitInfo headCommit) {
        Dialog<RollbackMode> dialog = new Dialog<>();
        dialog.setTitle("Rollback Last Commit");
        dialog.setHeaderText("Undo commit " + headCommit.shortHash());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Label info = new Label("""
                Commit: %s
                Author: %s
                Message: %s
                """.formatted(headCommit.shortHash(), headCommit.author(), headCommit.message()));

        ToggleGroup modeGroup = new ToggleGroup();

        RadioButton revertRb = new RadioButton("Revert (safe — creates a new undo commit)");
        revertRb.setToggleGroup(modeGroup);
        revertRb.setUserData(RollbackMode.REVERT);

        RadioButton softRb = new RadioButton("Reset soft (keep changes staged, local only)");
        softRb.setToggleGroup(modeGroup);
        softRb.setUserData(RollbackMode.RESET_SOFT);

        RadioButton hardRb = new RadioButton("Reset hard (discard all changes, destructive!)");
        hardRb.setToggleGroup(modeGroup);
        hardRb.setUserData(RollbackMode.RESET_HARD);

        // recommend revert if already pushed
        if (headCommit.isPushed()) {
            revertRb.setSelected(true);
            softRb.setDisable(true);
            hardRb.setDisable(true);
            info.setText(info.getText() + "\n⚠ This commit is already pushed. "
                    + "Only Revert is available.");
        } else {
            softRb.setSelected(true);
        }

        var content = new VBox(8, info, revertRb, softRb, hardRb);
        content.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Toggle sel = modeGroup.getSelectedToggle();
            return sel != null ? (RollbackMode) sel.getUserData() : null;
        });

        return dialog.showAndWait();
    }
}
```

### 9.4 `PullRequestDialog`

```java
package org.metalib.papifly.fx.github.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Optional;

/**
 * Modal dialog for creating a GitHub Pull Request.
 */
public final class PullRequestDialog {

    /** Result record. */
    public record Result(String title, String body, boolean openInBrowser) {}

    private PullRequestDialog() {}

    /**
     * @param headBranch the current (source) branch
     * @param baseBranch the target (default) branch
     * @return PR creation result, or empty if cancelled
     */
    public static Optional<Result> show(String headBranch, String baseBranch) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.setTitle("Create Pull Request");
        dialog.setHeaderText("Open a PR from " + headBranch + " → " + baseBranch);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField titleField = new TextField();
        titleField.setPromptText("PR title");

        TextArea bodyArea = new TextArea();
        bodyArea.setPromptText("Description (optional)");
        bodyArea.setPrefRowCount(6);
        bodyArea.setWrapText(true);

        Label headLabel = new Label(headBranch);
        Label baseLabel = new Label(baseBranch);

        CheckBox openBrowser = new CheckBox("Open in browser after creation");
        openBrowser.setSelected(true);

        var grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(8));
        grid.add(new Label("Head:"), 0, 0);
        grid.add(headLabel, 1, 0);
        grid.add(new Label("Base:"), 0, 1);
        grid.add(baseLabel, 1, 1);
        grid.add(new Label("Title:"), 0, 2);
        grid.add(titleField, 1, 2);
        GridPane.setHgrow(titleField, Priority.ALWAYS);

        var content = new VBox(8, grid, bodyArea, openBrowser);
        content.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().lookupButton(ButtonType.OK)
                .disableProperty()
                .bind(titleField.textProperty().isEmpty());

        dialog.setResultConverter(btn ->
                btn == ButtonType.OK
                        ? new Result(
                                titleField.getText().strip(),
                                bodyArea.getText() != null ? bodyArea.getText().strip() : "",
                                openBrowser.isSelected())
                        : null);

        return dialog.showAndWait();
    }
}
```

---

## 10 Host Application Integration

### 10.1 Mounting in the samples app

The samples module already wraps `DockManager.getRootPane()` in a `BorderPane`
for custom toolbars (see `PersistSample` pattern). The GitHub toolbar follows the
same approach:

```java
// In a new GitHubSample.java (or as a toolbar in the main SamplesApp)

DockManager dm = new DockManager();
dm.themeProperty().bind(themeProperty);
dm.setOwnerStage(ownerStage);

// Build some sample layout
var group = dm.createTabGroup();
group.addLeaf(dm.createLeaf("Welcome", new Label("Hello")));
dm.setRoot((DockElement) group);

// Create GitHub toolbar
var context = GitHubRepoContext.of(
        URI.create("https://github.com/org-metalib/papiflyfx-docking"),
        Path.of(".")
);
var credStore = new PatCredentialStore(System.getenv("GITHUB_TOKEN"));
var contribution = new GitHubToolbarContribution(context, credStore, dm.themeProperty());

// Mount
BorderPane wrapper = new BorderPane();
wrapper.setTop(contribution.toolbarNode());
wrapper.setCenter(dm.getRootPane());
return wrapper;
```

### 10.2 Cleanup

```java
// In the host's stop() or dispose path:
contribution.dispose();
dm.dispose();
```

---

## 11 UX Behaviour Constraints

### 11.1 Commit blocked on default branch

The commit button is disabled via a compound binding:

```java
commitBtn.disableProperty().bind(
    viewModel.onDefaultBranchProperty()          // current == default
        .or(viewModel.dirtyProperty().not())      // nothing to commit
        .or(viewModel.busyProperty())             // operation in flight
);
```

Default branch is determined by (in priority order):

1. GitHub API (`GET /repos/{owner}/{repo}` → `default_branch`)
2. JGit `ls-remote` to resolve remote HEAD
3. Heuristic: if `main` exists locally → `main`; else if `master` → `master`

### 11.2 Rollback safety

| Scenario | Recommended mode | UI behaviour |
|---|---|---|
| HEAD **not** pushed | RESET_SOFT (default selected) | All three modes enabled |
| HEAD **already** pushed | REVERT (only option) | Reset modes disabled, warning shown |

### 11.3 Dirty-tree branch checkout guard

Before switching branches, check `RepoStatus.isDirty()`. If true, show a
confirmation dialog offering:

- **Stash** changes (future enhancement)
- **Discard** changes (with hard reset warning)
- **Cancel** checkout

Initial implementation may simply block checkout with a warning.

### 11.4 Push authentication

Push requires credentials. The toolbar disables the Push and PR buttons when
`authenticatedProperty()` is false. The host app is responsible for providing a
`CredentialStore` with a valid token.

---

## 12 Error Handling & Concurrency

### 12.1 Never block the FX thread

All Git and HTTP operations execute on the daemon-thread `ExecutorService` in
`GitHubToolbarViewModel`. The UI binds to:

- `busyProperty()` → spinner + button disable
- `statusMessageProperty()` → progress text
- `lastErrorProperty()` → red error label

### 12.2 Error classification

| Error | Source | User message |
|---|---|---|
| Repository not found | `Git.open()` | "No Git repository at {path}" |
| Detached HEAD | `getCurrentBranch()` | "HEAD is detached — checkout a branch first" |
| Uncommitted changes | checkout guard | "Working tree has uncommitted changes" |
| Auth failure (401/403) | GitHub API | "Authentication failed — check your token" |
| Rate limited (429) | GitHub API | "GitHub rate limit reached — try again later" |
| Push rejected | JGit push | "Push rejected — pull and merge first" |
| Network unreachable | HttpClient | "Cannot reach GitHub — check network" |

### 12.3 Error recovery

`lastError` is cleared at the start of every new operation. The user can retry
any operation by clicking the button again. A future enhancement may add a
"Copy error details" button for diagnostics.

---

## 13 Testing Strategy

### 13.1 Unit tests (non-UI, no JavaFX toolkit)

| Test class | What it covers |
|---|---|
| `GitHubRepoContextTest` | URL parsing (HTTPS, SSH, edge cases) |
| `GitServiceTest` | All JGit operations on a temp `Git.init()` repo |
| `GitHubApiServiceTest` | JSON extraction, request building (mock `HttpClient`) |
| `RollbackModeTest` | Reset/revert semantics against temp repos |

Example `GitServiceTest`:

```java
package org.metalib.papifly.fx.github.service;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.github.model.RollbackMode;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GitServiceTest {

    @TempDir Path tempDir;
    Git bareGit;
    GitService service;

    @BeforeEach
    void setUp() throws Exception {
        bareGit = Git.init().setDirectory(tempDir.toFile()).call();
        // seed an initial commit so HEAD exists
        Files.writeString(tempDir.resolve("README.md"), "# test");
        bareGit.add().addFilepattern(".").call();
        bareGit.commit().setMessage("initial commit").call();

        service = new GitService(tempDir, null);
    }

    @AfterEach
    void tearDown() {
        service.close();
        bareGit.close();
    }

    @Test
    void getCurrentBranch_returnsMainAfterInit() throws Exception {
        // JGit defaults to "master" on init
        String branch = service.getCurrentBranch();
        assertTrue("master".equals(branch) || "main".equals(branch));
    }

    @Test
    void createAndCheckoutBranch_switchesToNewBranch() throws Exception {
        service.createAndCheckoutBranch("feature/test", "HEAD");
        assertEquals("feature/test", service.getCurrentBranch());
    }

    @Test
    void commit_createsNewHeadCommit() throws Exception {
        service.createAndCheckoutBranch("dev", "HEAD");
        Files.writeString(tempDir.resolve("file.txt"), "content");
        var info = service.commit("add file");
        assertNotNull(info.hash());
        assertEquals("add file", info.message());
    }

    @Test
    void rollbackSoft_keepsChangesStaged() throws Exception {
        service.createAndCheckoutBranch("dev", "HEAD");
        Files.writeString(tempDir.resolve("file.txt"), "content");
        service.commit("add file");

        service.rollbackLastCommit(RollbackMode.RESET_SOFT);

        var status = service.getStatus();
        assertTrue(status.isDirty(), "Changes should remain staged after soft reset");
    }

    @Test
    void rollbackHard_discardsChanges() throws Exception {
        service.createAndCheckoutBranch("dev", "HEAD");
        Files.writeString(tempDir.resolve("file.txt"), "content");
        service.commit("add file");

        service.rollbackLastCommit(RollbackMode.RESET_HARD);

        assertFalse(Files.exists(tempDir.resolve("file.txt")));
    }

    @Test
    void getStatus_detectsDirtyTree() throws Exception {
        Files.writeString(tempDir.resolve("new.txt"), "data");
        var status = service.getStatus();
        assertTrue(status.isDirty());
        assertFalse(status.untracked().isEmpty());
    }
}
```

### 13.2 UI tests (TestFX, headless)

```java
package org.metalib.papifly.fx.github;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.github.auth.PatCredentialStore;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.net.URI;

import static org.testfx.assertions.api.Assertions.assertThat;

@ExtendWith(ApplicationExtension.class)
class GitHubToolbarFxTest {

    private GitHubToolbar toolbar;

    @Start
    void start(Stage stage) {
        var ctx = GitHubRepoContext.remoteOnly(
                URI.create("https://github.com/org-metalib/papiflyfx-docking"));
        var creds = new PatCredentialStore();
        var theme = new SimpleObjectProperty<>(Theme.dark());
        toolbar = new GitHubToolbar(ctx, creds, theme);
        stage.setScene(new Scene(toolbar, 800, 40));
        stage.show();
    }

    @Test
    void toolbar_showsRepoLink(FxRobot robot) {
        assertThat(robot.lookup("org-metalib/papiflyfx-docking").queryLabeled())
                .isNotNull();
    }

    @Test
    void commitButton_disabledWhenNoLocalClone(FxRobot robot) {
        assertThat(robot.lookup("Commit…").queryButton()).isDisabled();
    }

    @Test
    void pushButton_disabledWhenNotAuthenticated(FxRobot robot) {
        assertThat(robot.lookup("Push").queryButton()).isDisabled();
    }
}
```

### 13.3 Running tests

```bash
# All module tests (headless)
./mvnw -pl papiflyfx-docking-github -am -Dtestfx.headless=true test

# Single test class
./mvnw -Dtest=GitServiceTest test -pl papiflyfx-docking-github -am

# UI tests only
./mvnw -Dtest=GitHubToolbarFxTest test -pl papiflyfx-docking-github -am -Dtestfx.headless=true
```

---

## 14 Implementation Phases

### Phase 1 — Scaffold & read-only UI

- Create module directory, POM, register in root aggregator
- Implement `GitHubRepoContext`, `BranchInfo`, `RepoStatus`
- Implement `GitService` (read-only: `getCurrentBranch`, `listBranches`, `getStatus`)
- Build `GitHubToolbar` UI with repo link, branch label, branch combo, dirty indicator
- Wire theme binding
- Unit tests for `GitService` and `GitHubRepoContext`
- UI test: toolbar renders, repo link visible

### Phase 2 — Branch operations

- Implement `GitService.checkoutBranch()` and `createAndCheckoutBranch()`
- Build `NewBranchDialog`
- Add dirty-tree checkout guard (confirmation dialog)
- Wire switch/new-branch buttons in toolbar
- Unit tests for branch operations
- UI test: switch branch updates label

### Phase 3 — Commit & rollback

- Implement `GitService.commit()`, `getHeadCommit()`, `rollbackLastCommit()`
- Implement `CommitInfo`, `RollbackMode`
- Build `CommitDialog` and `RollbackDialog`
- Add default-branch detection (`detectDefaultBranch()`)
- Enforce commit-disabled-on-default-branch binding
- Implement pushed-commit detection for rollback mode gating
- Unit tests for commit, rollback (soft/hard/revert)

### Phase 4 — Push & authentication

- Implement `CredentialStore` / `PatCredentialStore`
- Implement `GitService.push()`
- Wire Push button with auth guard
- Handle push errors (rejected, auth failure)
- Unit tests for push (against local bare remote)

### Phase 5 — Pull request creation

- Implement `GitHubApiService` (default branch query, PR creation)
- Build `PullRequestDialog`
- Wire Create PR button; open PR URL in browser on success
- Unit tests for API service (mocked HTTP)

### Phase 6 — Polish & hardening

- Retry/backoff for transient GitHub API errors
- Improved default-branch detection (GitHub API fallback → JGit `ls-remote` → heuristic)
- Protected-branch detection and clearer commit-disabled messaging
- Optional: OAuth device-flow auth for a richer desktop UX
- Optional: OS keychain integration for token storage
- Optional: stash support in dirty-tree checkout guard
- Documentation updates in `spec/` and module `README.md`

---

## 15 File Inventory (complete)

```
papiflyfx-docking-github/
├── pom.xml
└── src/
    ├── main/java/org/metalib/papifly/fx/github/
    │   ├── GitHubRepoContext.java
    │   ├── GitHubToolbar.java
    │   ├── GitHubToolbarContribution.java
    │   ├── auth/
    │   │   ├── CredentialStore.java
    │   │   └── PatCredentialStore.java
    │   ├── model/
    │   │   ├── BranchInfo.java
    │   │   ├── CommitInfo.java
    │   │   ├── RepoStatus.java
    │   │   └── RollbackMode.java
    │   ├── service/
    │   │   ├── GitHubApiService.java
    │   │   └── GitService.java
    │   └── ui/
    │       ├── GitHubToolbarViewModel.java
    │       ├── StatusIndicator.java
    │       ├── ToolbarRenderer.java
    │       └── dialog/
    │           ├── CommitDialog.java
    │           ├── NewBranchDialog.java
    │           ├── PullRequestDialog.java
    │           └── RollbackDialog.java
    └── test/java/org/metalib/papifly/fx/github/
        ├── GitHubRepoContextTest.java
        ├── GitHubToolbarFxTest.java
        └── service/
            ├── GitHubApiServiceTest.java
            └── GitServiceTest.java
```

---

## 16 Key Design Decisions Summary

| Decision | Rationale |
|---|---|
| **JGit over shelling out** | Cross-platform, structured exceptions, no external `git` binary dependency (ChatGPT, Grok) |
| **`java.net.http` over `github-api` library** | Zero extra dependencies; project convention avoids external JSON/HTTP libs (aligns with existing `DockSessionSerializer`) |
| **MVVM with `javafx.concurrent.Task`** | Never blocks FX thread; property bindings keep UI declarative (ChatGPT architecture) |
| **PAT-first authentication** | Simplest implementation; OAuth device-flow deferred to Phase 6 (ChatGPT recommendation) |
| **Toolbar as `HBox`, not `DockLeaf`** | The toolbar is a fixed UI element (top/bottom of window), not a dockable panel; `BorderPane.setTop/setBottom` is the natural mount point (Grok approach, refined) |
| **Module depends on API only (docks in test scope)** | Follows the `papiflyfx-docking-tree` / `papiflyfx-docking-code` pattern; minimises coupling |
| **Revert-only for pushed commits** | Prevents history rewrites on shared branches; reset available only for local-only commits (ChatGPT rollback semantics) |
| **No FXML, no CSS** | Project-wide convention: all programmatic JavaFX with `Theme` record for styling |

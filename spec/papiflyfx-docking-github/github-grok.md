### Overview of Development Approach

Developing the `papiflyfx-docking-github` module involves extending the PapiflyFX Docking Framework to create a custom dockable toolbar component that integrates Git and GitHub functionality. The framework's core (`papiflyfx-docking-docks`) handles docking layouts, allowing us to create a toolbar as a `DockLeaf` (a dockable panel) that can be positioned at the top or bottom via vertical splits. We'll use JGit for local Git operations (cloning detection, branches, commits, rollback, push) and the GitHub API Java client (e.g., org.kohsuke.github) for GitHub-specific actions like creating pull requests.

Key assumptions:
- The application root directory is a locally cloned Git repository (e.g., detected via `.git` folder).
- User provides a GitHub Personal Access Token (PAT) for authentication (stored securely, e.g., via app config).
- Commit is disabled on the `main`/`master` branch as per requirements.
- The toolbar is implemented as a horizontal JavaFX `ToolBar` or `HBox` wrapped in a `DockLeaf` for docking.
- Handle errors gracefully (e.g., no repo detected, network issues).

### Step 1: Set Up the New Module
Create a new Maven module in the PapiflyFX project structure:
- Module name: `papiflyfx-docking-github`
- Add to parent `pom.xml` under `<modules>`.
- Module `pom.xml`:
  ```xml
  <project>
    <parent>
      <groupId>org.metalib.papiflyfx</groupId>  <!-- Adjust based on parent -->
      <artifactId>papiflyfx-docking-parent</artifactId>
      <version>1.0-SNAPSHOT</version>
    </parent>
    <artifactId>papiflyfx-docking-github</artifactId>
    <dependencies>
      <dependency>
        <groupId>org.metalib.papiflyfx</groupId>
        <artifactId>papiflyfx-docking-api</artifactId>
        <version>${project.version}</version>
      </dependency>
      <dependency>
        <groupId>org.metalib.papiflyfx</groupId>
        <artifactId>papiflyfx-docking-docks</artifactId>
        <version>${project.version}</version>
      </dependency>
      <dependency>  <!-- JGit for Git ops -->
        <groupId>org.eclipse.jgit</groupId>
        <artifactId>org.eclipse.jgit</artifactId>
        <version>6.9.0.202403050828-r</version>  <!-- Latest as of 2026 -->
      </dependency>
      <dependency>  <!-- GitHub API client -->
        <groupId>org.kohsuke</groupId>
        <artifactId>github-api</artifactId>
        <version>1.323</version>  <!-- Latest as of 2026 -->
      </dependency>
      <dependency>  <!-- JavaFX (already in parent, but ensure) -->
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>23.0.1</version>
      </dependency>
    </dependencies>
  </project>
  ```
- Build: Run `mvn clean package` from root.

### Step 2: Implement the GitHub Toolbar Component
Create a class `GitHubDockingToolbar` that extends `ToolBar` (for native JavaFX toolbar styling) and implements the required features.

- **Core Logic**:
    - Detect local repo: Use `Git.open(new File("."))` to open the current directory as a Git repo.
    - Get repo URL: `git.getRepository().getConfig().getString("remote", "origin", "url")` – parse to extract GitHub owner/repo (e.g., from `git@github.com:owner/repo.git`).
    - Current branch: `git.getRepository().getBranch()`.
    - List branches: `git.branchList().call()` for local branches.
    - Auth: Use a PAT for GitHub API and JGit push (via `UsernamePasswordCredentialsProvider` for HTTPS).

- **UI Elements**:
    - Hyperlink: Clickable link to GitHub repo (e.g., `https://github.com/owner/repo`).
    - Label: Display current branch (update on changes).
    - ComboBox: List branches for switching.
    - Buttons: "Switch Branch", "New Branch", "Commit", "Rollback Last Commit", "Push", "Create PR".
    - Dialogs: Use `TextInputDialog` or custom for branch names, commit messages, PR titles/body.

Example skeleton in `src/main/java/org/metalib/papiflyfx/docking/github/GitHubDockingToolbar.java`:

```java
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GHRepository;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

public class GitHubDockingToolbar extends ToolBar {
    private Git git;
    private String pat;  // Personal Access Token (inject via constructor or config)
    private GitHub githubClient;
    private String repoUrl;
    private String currentBranch;
    private Label branchLabel;
    private ComboBox<String> branchComboBox;
    private GHRepository ghRepo;

    public GitHubDockingToolbar(String pat) {
        this.pat = pat;
        initRepo();
        initUI();
        initGitHubClient();
    }

    private void initRepo() {
        try {
            git = Git.open(new File("."));
            repoUrl = git.getRepository().getConfig().getString("remote", "origin", "url");
            currentBranch = git.getRepository().getBranch();
            // Parse repo name, e.g., "owner/repo" from repoUrl
            String repoName = extractRepoName(repoUrl);  // Implement extraction logic
            ghRepo = githubClient.getRepository(repoName);
        } catch (IOException e) {
            // Handle no repo: disable features, show error label
        }
    }

    private void initUI() {
        Hyperlink repoLink = new Hyperlink("GitHub Repo");
        repoLink.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(URI.create("https://github.com/" + extractRepoName(repoUrl)));
            } catch (Exception ex) { /* Handle */ }
        });

        branchLabel = new Label("Branch: " + (currentBranch != null ? currentBranch : "N/A"));

        branchComboBox = new ComboBox<>();
        updateBranchList();

        Button switchBranchBtn = new Button("Switch Branch");
        switchBranchBtn.setOnAction(e -> switchBranch(branchComboBox.getValue()));

        Button newBranchBtn = new Button("New Branch");
        newBranchBtn.setOnAction(e -> createNewBranch());

        Button commitBtn = new Button("Commit");
        commitBtn.setDisable(isMainBranch());  // Disable if on main
        commitBtn.setOnAction(e -> commitChanges());

        Button rollbackBtn = new Button("Rollback Last");
        rollbackBtn.setOnAction(e -> rollbackLastCommit());

        Button pushBtn = new Button("Push");
        pushBtn.setOnAction(e -> pushChanges());

        Button prBtn = new Button("Create PR");
        prBtn.setOnAction(e -> createPullRequest());

        getItems().addAll(repoLink, branchLabel, branchComboBox, switchBranchBtn, newBranchBtn, commitBtn, rollbackBtn, pushBtn, prBtn);
    }

    private void initGitHubClient() {
        try {
            githubClient = GitHub.connectUsingOAuth(pat);
        } catch (IOException e) { /* Handle auth failure */ }
    }

    private void updateBranchList() {
        try {
            List<Ref> branches = git.branchList().call();
            branchComboBox.getItems().clear();
            for (Ref ref : branches) {
                branchComboBox.getItems().add(ref.getName().replace("refs/heads/", ""));
            }
        } catch (GitAPIException e) { /* Handle */ }
    }

    private void switchBranch(String branch) {
        try {
            git.checkout().setName(branch).call();
            currentBranch = branch;
            branchLabel.setText("Branch: " + branch);
            updateBranchList();
        } catch (GitAPIException e) { /* Handle */ }
    }

    private void createNewBranch() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Branch");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                git.checkout().setCreateBranch(true).setName(name).call();
                updateBranchList();
                switchBranch(name);
            } catch (GitAPIException e) { /* Handle */ }
        });
    }

    private void commitChanges() {
        if (isMainBranch()) return;  // Enforce no commit to main
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Commit Message");
        Optional<String> message = dialog.showAndWait();
        message.ifPresent(msg -> {
            try {
                git.add().addFilepattern(".").call();  // Stage all
                git.commit().setMessage(msg).call();
            } catch (GitAPIException e) { /* Handle */ }
        });
    }

    private boolean isMainBranch() {
        return "main".equals(currentBranch) || "master".equals(currentBranch);
    }

    private void rollbackLastCommit() {
        try {
            git.reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD~1").call();  // Or use revert for new commit
        } catch (GitAPIException e) { /* Handle */ }
    }

    private void pushChanges() {
        try {
            git.push().setCredentialsProvider(new Username
```
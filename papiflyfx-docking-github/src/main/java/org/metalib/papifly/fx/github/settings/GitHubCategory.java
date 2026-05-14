package org.metalib.papifly.fx.github.settings;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.settings.api.SecretKeyNames;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.ui.UiMetrics;

import java.util.List;

public class GitHubCategory implements SettingsCategory {

    private static final SettingDefinition<String> HOST_DEFINITION = SettingDefinition
        .of("github.host", "GitHub Host", SettingType.STRING, "https://api.github.com")
        .withDescription("Base API URL used for GitHub requests.");
    private static final SettingDefinition<String> AUTHOR_NAME_DEFINITION = SettingDefinition
        .of("github.commit.author.name", "Commit Author Name", SettingType.STRING, "")
        .withDescription("Default commit author name for GitHub workflows.");
    private static final SettingDefinition<String> AUTHOR_EMAIL_DEFINITION = SettingDefinition
        .of("github.commit.author.email", "Commit Author Email", SettingType.STRING, "")
        .withDescription("Default commit author email for GitHub workflows.");
    private static final SettingDefinition<String> PAT_DEFINITION = SettingDefinition
        .of("github.pat", "Personal Access Token", SettingType.SECRET, "")
        .asSecret()
        .withDescription("Stored in the shared secret store.");

    private TextField hostField;
    private TextField authorNameField;
    private TextField authorEmailField;
    private PasswordField patField;
    private CheckBox revealPat;
    private VBox pane;
    private boolean dirty;

    @Override
    public String id() {
        return "github";
    }

    @Override
    public String displayName() {
        return "GitHub";
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public List<SettingDefinition<?>> definitions() {
        return List.of(HOST_DEFINITION, AUTHOR_NAME_DEFINITION, AUTHOR_EMAIL_DEFINITION, PAT_DEFINITION);
    }

    @Override
    public Node buildSettingsPane(SettingsContext context) {
        if (pane == null) {
            hostField = compactField(new TextField());
            authorNameField = compactField(new TextField());
            authorEmailField = compactField(new TextField());
            patField = compactField(new PasswordField());
            revealPat = settingsCheckBox(new CheckBox("Reveal token"));

            revealPat.selectedProperty().addListener((obs, oldValue, newValue) -> {
                patField.setPromptText(newValue ? patField.getText() : "");
                dirty = true;
            });
            hostField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            authorNameField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            authorEmailField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            patField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);

            pane = new VBox(
                UiMetrics.SPACE_3,
                field("GitHub Host", hostField),
                field("Commit Author Name", authorNameField),
                field("Commit Author Email", authorEmailField),
                field("Personal Access Token", patField),
                revealPat
            );
            pane.setPadding(new Insets(UiMetrics.SPACE_2));
        }
        reset(context);
        return pane;
    }

    @Override
    public void apply(SettingsContext context) {
        context.storage().putString(SettingScope.APPLICATION, HOST_DEFINITION.key(), hostField.getText());
        context.storage().putString(SettingScope.APPLICATION, AUTHOR_NAME_DEFINITION.key(), authorNameField.getText());
        context.storage().putString(SettingScope.APPLICATION, AUTHOR_EMAIL_DEFINITION.key(), authorEmailField.getText());
        context.secretStore().setSecret(SecretKeyNames.githubPat(), patField.getText());
        context.storage().save();
        dirty = false;
    }

    @Override
    public void reset(SettingsContext context) {
        hostField.setText(context.storage().getString(SettingScope.APPLICATION, HOST_DEFINITION.key(), HOST_DEFINITION.defaultValue()));
        authorNameField.setText(context.storage().getString(SettingScope.APPLICATION, AUTHOR_NAME_DEFINITION.key(), AUTHOR_NAME_DEFINITION.defaultValue()));
        authorEmailField.setText(context.storage().getString(SettingScope.APPLICATION, AUTHOR_EMAIL_DEFINITION.key(), AUTHOR_EMAIL_DEFINITION.defaultValue()));
        patField.setText(context.secretStore().getSecret(SecretKeyNames.githubPat()).orElse(""));
        dirty = false;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    private VBox field(String labelText, Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("pf-settings-control-title");
        return new VBox(UiMetrics.SPACE_1, label, field);
    }

    private static <T extends TextField> T compactField(T field) {
        field.getStyleClass().add("pf-ui-compact-field");
        return field;
    }

    private static <T extends CheckBox> T settingsCheckBox(T checkBox) {
        checkBox.getStyleClass().add("pf-settings-check-box");
        return checkBox;
    }
}

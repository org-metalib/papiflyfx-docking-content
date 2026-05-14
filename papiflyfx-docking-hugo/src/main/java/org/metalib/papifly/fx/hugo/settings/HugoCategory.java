package org.metalib.papifly.fx.hugo.settings;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.ui.UiMetrics;

import java.util.List;

public class HugoCategory implements SettingsCategory {

    private static final SettingDefinition<String> BINARY_DEFINITION = SettingDefinition
        .of("hugo.binary", "Hugo Binary", SettingType.STRING, "hugo")
        .withDescription("CLI binary used to launch Hugo.");
    private static final SettingDefinition<Integer> PORT_DEFINITION = SettingDefinition
        .of("hugo.port", "Preferred Port", SettingType.INTEGER, 1313)
        .withScope(SettingScope.WORKSPACE)
        .withDescription("Preferred workspace port for preview servers.");
    private static final SettingDefinition<String> BIND_ADDRESS_DEFINITION = SettingDefinition
        .of("hugo.bindAddress", "Bind Address", SettingType.STRING, "127.0.0.1")
        .withScope(SettingScope.WORKSPACE)
        .withDescription("Address used for the preview server.");
    private static final SettingDefinition<Boolean> BUILD_DRAFTS_DEFINITION = SettingDefinition
        .of("hugo.buildDrafts", "Build Drafts", SettingType.BOOLEAN, true)
        .withScope(SettingScope.WORKSPACE)
        .withDescription("Include draft content when running preview.");

    private TextField binaryField;
    private TextField portField;
    private TextField bindAddressField;
    private CheckBox buildDraftsBox;
    private VBox pane;
    private boolean dirty;

    @Override
    public String id() {
        return "hugo";
    }

    @Override
    public String displayName() {
        return "Hugo";
    }

    @Override
    public int order() {
        return 60;
    }

    @Override
    public List<SettingDefinition<?>> definitions() {
        return List.of(BINARY_DEFINITION, PORT_DEFINITION, BIND_ADDRESS_DEFINITION, BUILD_DRAFTS_DEFINITION);
    }

    @Override
    public Node buildSettingsPane(SettingsContext context) {
        if (pane == null) {
            binaryField = compactField(new TextField());
            portField = compactField(new TextField());
            bindAddressField = compactField(new TextField());
            buildDraftsBox = settingsCheckBox(new CheckBox("Include drafts"));

            binaryField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            portField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            bindAddressField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            buildDraftsBox.selectedProperty().addListener((obs, oldValue, newValue) -> dirty = true);

            pane = new VBox(
                UiMetrics.SPACE_3,
                field("Hugo Binary", binaryField),
                field("Preferred Port", portField),
                field("Bind Address", bindAddressField),
                buildDraftsBox
            );
            pane.setPadding(new Insets(UiMetrics.SPACE_2));
        }
        reset(context);
        return pane;
    }

    @Override
    public void apply(SettingsContext context) {
        context.storage().putString(SettingScope.APPLICATION, BINARY_DEFINITION.key(), binaryField.getText());
        context.storage().putInt(SettingScope.WORKSPACE, PORT_DEFINITION.key(), parse(portField.getText(), 1313));
        context.storage().putString(SettingScope.WORKSPACE, BIND_ADDRESS_DEFINITION.key(), bindAddressField.getText());
        context.storage().putBoolean(SettingScope.WORKSPACE, BUILD_DRAFTS_DEFINITION.key(), buildDraftsBox.isSelected());
        context.storage().save();
        dirty = false;
    }

    @Override
    public void reset(SettingsContext context) {
        binaryField.setText(context.storage().getString(SettingScope.APPLICATION, BINARY_DEFINITION.key(), BINARY_DEFINITION.defaultValue()));
        portField.setText(String.valueOf(context.storage().getInt(SettingScope.WORKSPACE, PORT_DEFINITION.key(), PORT_DEFINITION.defaultValue())));
        bindAddressField.setText(context.storage().getString(SettingScope.WORKSPACE, BIND_ADDRESS_DEFINITION.key(), BIND_ADDRESS_DEFINITION.defaultValue()));
        buildDraftsBox.setSelected(context.storage().getBoolean(SettingScope.WORKSPACE, BUILD_DRAFTS_DEFINITION.key(), BUILD_DRAFTS_DEFINITION.defaultValue()));
        dirty = false;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    private VBox field(String labelText, TextField field) {
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

    private int parse(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}

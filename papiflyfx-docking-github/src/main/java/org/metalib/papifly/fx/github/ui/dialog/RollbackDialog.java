package org.metalib.papifly.fx.github.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.github.model.RollbackMode;
import org.metalib.papifly.fx.ui.UiMetrics;

public class RollbackDialog extends Dialog<RollbackMode> {

    public RollbackDialog(boolean headPushed) {
        this(headPushed, null);
    }

    public RollbackDialog(boolean headPushed, Theme theme) {
        setTitle("Rollback Last Commit");

        ButtonType applyType = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(applyType, ButtonType.CANCEL);

        ToggleGroup toggleGroup = new ToggleGroup();
        RadioButton revert = new RadioButton("Revert");
        revert.setToggleGroup(toggleGroup);
        revert.setUserData(RollbackMode.REVERT);
        revert.setSelected(true);

        RadioButton soft = new RadioButton("Reset soft");
        soft.setToggleGroup(toggleGroup);
        soft.setUserData(RollbackMode.RESET_SOFT);

        RadioButton hard = new RadioButton("Reset hard");
        hard.setToggleGroup(toggleGroup);
        hard.setUserData(RollbackMode.RESET_HARD);

        if (headPushed) {
            soft.setDisable(true);
            hard.setDisable(true);
        }

        VBox content = new VBox(UiMetrics.SPACE_2, revert, soft, hard);
        content.setPadding(new Insets(UiMetrics.SPACE_3));
        getDialogPane().setContent(content);

        GitHubDialogStyler.apply(this, theme);

        setResultConverter(buttonType -> {
            if (buttonType != applyType || toggleGroup.getSelectedToggle() == null) {
                return null;
            }
            return (RollbackMode) toggleGroup.getSelectedToggle().getUserData();
        });
    }
}

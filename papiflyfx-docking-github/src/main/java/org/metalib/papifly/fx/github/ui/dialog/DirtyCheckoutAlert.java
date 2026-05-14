package org.metalib.papifly.fx.github.ui.dialog;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.metalib.papifly.fx.docking.api.Theme;

public final class DirtyCheckoutAlert {

    private DirtyCheckoutAlert() {
    }

    public static boolean confirm(String branchName) {
        return confirm(branchName, null);
    }

    public static boolean confirm(String branchName, Theme theme) {
        Alert alert = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Working tree has changes. Force checkout to \"" + branchName + "\"?",
            ButtonType.OK,
            ButtonType.CANCEL
        );
        alert.setTitle("Force checkout");
        GitHubDialogStyler.apply(alert, theme, ButtonType.OK);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}

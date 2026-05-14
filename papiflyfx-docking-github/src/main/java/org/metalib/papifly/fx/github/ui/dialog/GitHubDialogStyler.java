package org.metalib.papifly.fx.github.ui.dialog;

import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.github.ui.theme.GitHubThemeSupport;
import org.metalib.papifly.fx.github.ui.theme.GitHubToolbarTheme;
import org.metalib.papifly.fx.github.ui.theme.GitHubToolbarThemeMapper;

import java.util.Set;

final class GitHubDialogStyler {

    private GitHubDialogStyler() {
    }

    static void apply(Dialog<?> dialog, Theme baseTheme, ButtonType... dangerButtons) {
        DialogPane dialogPane = dialog.getDialogPane();
        GitHubToolbarTheme theme = GitHubToolbarThemeMapper.map(baseTheme);
        GitHubThemeSupport.ensureStylesheetLoaded(dialogPane, GitHubThemeSupport.DIALOG_STYLESHEET);
        if (!dialogPane.getStyleClass().contains("pf-github-dialog-pane")) {
            dialogPane.getStyleClass().add("pf-github-dialog-pane");
        }
        dialogPane.setStyle(GitHubThemeSupport.themeVariables(theme));
        Set<ButtonType> dangerSet = Set.of(dangerButtons);
        styleButtons(dialogPane, dangerSet, theme.buttonHeight());
        dialog.setOnShowing(event -> styleButtons(dialogPane, dangerSet, theme.buttonHeight()));
    }

    private static void styleButtons(DialogPane dialogPane, Set<ButtonType> dangerButtons, double buttonHeight) {
        for (ButtonType buttonType : dialogPane.getButtonTypes()) {
            Node node = dialogPane.lookupButton(buttonType);
            if (node == null) {
                continue;
            }
            node.getStyleClass().removeAll(
                "pf-github-dialog-button",
                "pf-github-dialog-button-primary",
                "pf-github-dialog-button-secondary",
                "pf-github-dialog-button-danger"
            );
            node.getStyleClass().add("pf-github-dialog-button");
            if (dangerButtons.contains(buttonType)) {
                node.getStyleClass().add("pf-github-dialog-button-danger");
                continue;
            }
            ButtonBar.ButtonData buttonData = buttonType.getButtonData();
            if (buttonData == ButtonBar.ButtonData.OK_DONE
                || buttonData == ButtonBar.ButtonData.YES
                || buttonData == ButtonBar.ButtonData.FINISH) {
                node.getStyleClass().add("pf-github-dialog-button-primary");
            } else {
                node.getStyleClass().add("pf-github-dialog-button-secondary");
            }
            node.setStyle(String.format(java.util.Locale.ROOT,
                "-fx-min-height: %.1fpx; -fx-pref-height: %.1fpx; -fx-max-height: %.1fpx;",
                buttonHeight,
                buttonHeight,
                buttonHeight));
        }
    }
}

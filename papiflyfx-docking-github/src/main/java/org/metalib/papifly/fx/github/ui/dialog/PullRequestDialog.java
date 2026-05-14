package org.metalib.papifly.fx.github.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.github.model.PullRequestDraft;
import org.metalib.papifly.fx.ui.UiMetrics;

public class PullRequestDialog extends Dialog<PullRequestDraft> {

    public PullRequestDialog(String currentBranch, String defaultBranch) {
        this(currentBranch, defaultBranch, null);
    }

    public PullRequestDialog(String currentBranch, String defaultBranch, Theme theme) {
        setTitle("Create Pull Request");

        ButtonType createType = new ButtonType("Create PR", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);

        TextField titleField = new TextField("PR: " + currentBranch);
        TextField headField = new TextField(currentBranch);
        TextField baseField = new TextField(defaultBranch);
        TextArea bodyArea = new TextArea();
        bodyArea.setPrefRowCount(6);
        CheckBox openBrowser = new CheckBox("Open PR in browser after create");
        openBrowser.setSelected(true);

        GridPane grid = new GridPane();
        grid.setHgap(UiMetrics.SPACE_2);
        grid.setVgap(UiMetrics.SPACE_2);
        grid.setPadding(new Insets(UiMetrics.SPACE_3));
        grid.addRow(0, new Label("Title"), titleField);
        grid.addRow(1, new Label("Head"), headField);
        grid.addRow(2, new Label("Base"), baseField);
        grid.addRow(3, new Label("Body"), bodyArea);
        grid.add(openBrowser, 1, 4);

        getDialogPane().setContent(grid);
        GitHubDialogStyler.apply(this, theme);

        setResultConverter(buttonType -> {
            if (buttonType != createType) {
                return null;
            }
            String title = titleField.getText() == null ? "" : titleField.getText().trim();
            String body = bodyArea.getText() == null ? "" : bodyArea.getText().trim();
            String head = headField.getText() == null ? "" : headField.getText().trim();
            String base = baseField.getText() == null ? "" : baseField.getText().trim();
            if (title.isBlank() || head.isBlank() || base.isBlank()) {
                return null;
            }
            return new PullRequestDraft(title, body, head, base, openBrowser.isSelected());
        });
    }
}

package org.metalib.papifly.fx.github.ui.dialog;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.github.model.BranchRef;
import org.metalib.papifly.fx.ui.UiMetrics;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public class NewBranchDialog extends Dialog<NewBranchDialog.Result> {

    public NewBranchDialog(List<BranchRef> branches, String selectedStartPoint) {
        this(branches, selectedStartPoint, null);
    }

    public NewBranchDialog(List<BranchRef> branches, String selectedStartPoint, Theme theme) {
        setTitle("Create Branch");

        ButtonType createType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("feature/new-branch");

        LinkedHashSet<String> branchNames = new LinkedHashSet<>();
        for (BranchRef branch : branches) {
            branchNames.add(branch.name());
        }
        ComboBox<String> startPoint = new ComboBox<>(FXCollections.observableArrayList(branchNames));
        startPoint.setEditable(true);
        if (selectedStartPoint != null && !selectedStartPoint.isBlank()) {
            startPoint.setValue(selectedStartPoint);
        } else if (!branchNames.isEmpty()) {
            startPoint.setValue(branchNames.iterator().next());
        }

        GridPane grid = new GridPane();
        grid.setHgap(UiMetrics.SPACE_2);
        grid.setVgap(UiMetrics.SPACE_2);
        grid.setPadding(new Insets(UiMetrics.SPACE_3));
        grid.addRow(0, new Label("Name"), nameField);
        grid.addRow(1, new Label("Start Point"), startPoint);

        getDialogPane().setContent(grid);
        GitHubDialogStyler.apply(this, theme);

        setResultConverter(buttonType -> {
            if (buttonType != createType) {
                return null;
            }
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            String base = Optional.ofNullable(startPoint.getEditor().getText()).orElse("").trim();
            if (name.isBlank()) {
                return null;
            }
            return new Result(name, base);
        });
    }

    public record Result(String name, String startPoint) {
    }
}

package org.metalib.papifly.fx.github.ui.toolbar;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import org.metalib.papifly.fx.github.model.CurrentRefState;
import org.metalib.papifly.fx.github.model.GitRefKind;
import org.metalib.papifly.fx.ui.UiPillButton;

import java.util.List;

public class RefPill extends UiPillButton {

    private static final List<String> DOT_VARIANTS = List.of(
        "pf-github-ref-dot-clean",
        "pf-github-ref-dot-dirty",
        "pf-github-ref-dot-neutral"
    );

    private static final List<String> KIND_VARIANTS = List.of(
        "pf-github-ref-kind-local",
        "pf-github-ref-kind-remote",
        "pf-github-ref-kind-tag",
        "pf-github-ref-kind-detached"
    );

    private final Circle statusDot;
    private final Label kindLabel;
    private final Label textLabel;
    private final Label chevronLabel;

    public RefPill() {
        super();
        setId("github-ref-pill");
        getStyleClass().add("pf-github-ref-pill");
        setMnemonicParsing(false);
        setFocusTraversable(true);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        statusDot = new Circle(4);
        statusDot.getStyleClass().addAll("pf-github-ref-dot", "pf-github-ref-dot-neutral");

        kindLabel = new Label("R");
        kindLabel.getStyleClass().addAll("pf-github-ref-kind", "pf-github-ref-kind-remote");

        textLabel = new Label("main");
        textLabel.getStyleClass().add("pf-github-ref-text");

        chevronLabel = new Label("v");
        chevronLabel.getStyleClass().add("pf-github-ref-chevron");

        HBox content = new HBox(statusDot, kindLabel, textLabel, chevronLabel);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("pf-github-ref-pill-content");
        setGraphic(content);
    }

    public void update(CurrentRefState refState) {
        textLabel.setText(refState.displayName());
        updateKind(refState.kind());
        updateDot(refState.statusDotState());
    }

    private void updateKind(GitRefKind kind) {
        kindLabel.getStyleClass().removeAll(KIND_VARIANTS);
        switch (kind) {
            case LOCAL_BRANCH -> {
                kindLabel.setText("B");
                kindLabel.getStyleClass().add("pf-github-ref-kind-local");
            }
            case REMOTE_BRANCH -> {
                kindLabel.setText("R");
                kindLabel.getStyleClass().add("pf-github-ref-kind-remote");
            }
            case TAG -> {
                kindLabel.setText("T");
                kindLabel.getStyleClass().add("pf-github-ref-kind-tag");
            }
            case DETACHED_COMMIT -> {
                kindLabel.setText("C");
                kindLabel.getStyleClass().add("pf-github-ref-kind-detached");
            }
        }
    }

    private void updateDot(CurrentRefState.StatusDotState statusDotState) {
        statusDot.getStyleClass().removeAll(DOT_VARIANTS);
        switch (statusDotState) {
            case CLEAN -> statusDot.getStyleClass().add("pf-github-ref-dot-clean");
            case DIRTY -> statusDot.getStyleClass().add("pf-github-ref-dot-dirty");
            case NEUTRAL -> statusDot.getStyleClass().add("pf-github-ref-dot-neutral");
        }
    }
}

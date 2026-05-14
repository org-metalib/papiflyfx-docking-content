package org.metalib.papifly.fx.github.ui.popup;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import org.metalib.papifly.fx.github.model.RefPopupEntry;
import org.metalib.papifly.fx.github.model.RefPopupSection;
import org.metalib.papifly.fx.github.ui.GitHubToolbarViewModel;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class GitRefPopupController {

    private final GitHubToolbarViewModel viewModel;
    private final GitRefPopup popup;
    private final Consumer<RefPopupEntry.Action> actionHandler;
    private final Consumer<RefPopupEntry.Ref> refHandler;

    public GitRefPopupController(
        GitHubToolbarViewModel viewModel,
        GitRefPopup popup,
        Consumer<RefPopupEntry.Action> actionHandler,
        Consumer<RefPopupEntry.Ref> refHandler
    ) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        this.popup = Objects.requireNonNull(popup, "popup");
        this.actionHandler = Objects.requireNonNull(actionHandler, "actionHandler");
        this.refHandler = Objects.requireNonNull(refHandler, "refHandler");

        popup.searchField().textProperty().bindBidirectional(viewModel.refPopupFilterProperty());
        popup.setOnAction(action -> {
            popup.hide();
            actionHandler.accept(action);
        });
        popup.setOnRefActivated(ref -> {
            popup.hide();
            refHandler.accept(ref);
        });
        popup.setOnHidden(viewModel::clearRefPopupFilter);
        popup.setSections(viewModel.refPopupSectionsProperty());
        viewModel.refPopupSectionsProperty().addListener((ListChangeListener<RefPopupSection>) change ->
            popup.setSections(viewModel.refPopupSectionsProperty()));
    }

    public void toggle(Node anchor) {
        if (popup.isShowing()) {
            popup.hide();
            return;
        }
        show(anchor);
    }

    public void show(Node anchor) {
        popup.setSections(viewModel.refPopupSectionsProperty());
        popup.show(anchor);
    }

    public void hide() {
        popup.hide();
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    public void setSections(List<RefPopupSection> sections) {
        popup.setSections(sections);
    }
}

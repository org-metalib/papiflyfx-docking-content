package org.metalib.papifly.fx.media.controls;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.theme.MediaThemeMapper;
import org.metalib.papifly.fx.ui.UiMetrics;

import java.util.function.Consumer;

public class ZoomControls extends HBox {

    private static final double ICON_SIZE = UiMetrics.SPACE_4;
    private static final double HALF_STROKE = UiMetrics.SPACE_1 * 0.5;

    private final Label zoomLabel = new Label("100%");
    private final Canvas plusBtn  = new Canvas(ICON_SIZE, ICON_SIZE);
    private final Canvas minusBtn = new Canvas(ICON_SIZE, ICON_SIZE);
    private final Canvas resetBtn = new Canvas(ICON_SIZE, ICON_SIZE);

    public ZoomControls(Consumer<Double> onZoom, Runnable onReset) {
        setAlignment(Pos.CENTER);
        setSpacing(UiMetrics.SPACE_1);
        setPadding(new Insets(UiMetrics.SPACE_1, UiMetrics.SPACE_2, UiMetrics.SPACE_1, UiMetrics.SPACE_2));
        getChildren().addAll(minusBtn, zoomLabel, plusBtn, resetBtn);

        minusBtn.setOnMouseClicked(e -> onZoom.accept(-0.1));
        plusBtn.setOnMouseClicked(e  -> onZoom.accept(+0.1));
        resetBtn.setOnMouseClicked(e -> onReset.run());
    }

    public void setZoomLevel(double level) {
        zoomLabel.setText((int)(level * 100) + "%");
    }

    public void applyTheme(Theme t) {
        Color c = MediaThemeMapper.toColor(MediaThemeMapper.controlForeground(t));
        zoomLabel.setFont(t.contentFont());
        zoomLabel.setTextFill(c);
        paintPlus(c);
        paintMinus(c);
        paintReset(c);
    }

    private void paintPlus(Color c) {
        GraphicsContext gc = plusBtn.getGraphicsContext2D();
        gc.clearRect(0, 0, ICON_SIZE, ICON_SIZE);
        gc.setFill(c);
        gc.fillRect((ICON_SIZE - HALF_STROKE) * 0.5, UiMetrics.SPACE_1 * 0.5, HALF_STROKE, ICON_SIZE - UiMetrics.SPACE_1);
        gc.fillRect(UiMetrics.SPACE_1 * 0.5, (ICON_SIZE - HALF_STROKE) * 0.5, ICON_SIZE - UiMetrics.SPACE_1, HALF_STROKE);
    }

    private void paintMinus(Color c) {
        GraphicsContext gc = minusBtn.getGraphicsContext2D();
        gc.clearRect(0, 0, ICON_SIZE, ICON_SIZE);
        gc.setFill(c);
        gc.fillRect(UiMetrics.SPACE_1 * 0.5, (ICON_SIZE - HALF_STROKE) * 0.5, ICON_SIZE - UiMetrics.SPACE_1, HALF_STROKE);
    }

    private void paintReset(Color c) {
        GraphicsContext gc = resetBtn.getGraphicsContext2D();
        gc.clearRect(0, 0, ICON_SIZE, ICON_SIZE);
        gc.setStroke(c);
        gc.setLineWidth(HALF_STROKE);
        gc.strokeOval(UiMetrics.SPACE_1 * 0.5, UiMetrics.SPACE_1 * 0.5, ICON_SIZE - UiMetrics.SPACE_1, ICON_SIZE - UiMetrics.SPACE_1);
    }
}

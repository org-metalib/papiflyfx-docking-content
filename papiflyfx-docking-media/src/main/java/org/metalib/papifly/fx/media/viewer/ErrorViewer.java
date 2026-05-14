package org.metalib.papifly.fx.media.viewer;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.theme.MediaThemeMapper;

public class ErrorViewer extends StackPane {

    private final Canvas icon  = new Canvas(48, 48);
    private final Label  label = new Label();

    public ErrorViewer(String message) {
        setAlignment(Pos.CENTER);
        label.setText(message);
        label.setWrapText(true);
        getChildren().addAll(icon, label);
    }

    public void applyTheme(Theme t) {
        Color c = MediaThemeMapper.toColor(MediaThemeMapper.controlForeground(t));
        label.setFont(t.contentFont());
        label.setTextFill(c);
        paintBrokenIcon(c);
    }

    private void paintBrokenIcon(Color c) {
        GraphicsContext gc = icon.getGraphicsContext2D();
        gc.clearRect(0, 0, 48, 48);
        gc.setStroke(c);
        gc.setLineWidth(2.0);
        gc.strokeRect(6, 6, 36, 36);
        gc.strokeLine(6, 24, 42, 24);
        gc.strokeLine(20, 6, 20, 24);
        gc.strokeLine(6, 42, 42, 6);
    }
}

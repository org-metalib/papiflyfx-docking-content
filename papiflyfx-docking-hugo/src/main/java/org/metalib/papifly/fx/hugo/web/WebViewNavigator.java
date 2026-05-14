package org.metalib.papifly.fx.hugo.web;

import javafx.scene.web.WebEngine;

public final class WebViewNavigator {

    private final WebEngine webEngine;

    public WebViewNavigator(WebEngine webEngine) {
        this.webEngine = webEngine;
    }

    public void navigate(String location) {
        if (location == null || location.isBlank()) {
            return;
        }
        webEngine.load(location);
    }

    public void reload() {
        webEngine.reload();
    }

    public void back() {
        executeHistoryScript("history.back()");
    }

    public void forward() {
        executeHistoryScript("history.forward()");
    }

    public String currentLocation() {
        return webEngine.getLocation();
    }

    private void executeHistoryScript(String script) {
        try {
            webEngine.executeScript(script);
        } catch (Exception ignored) {
        }
    }
}

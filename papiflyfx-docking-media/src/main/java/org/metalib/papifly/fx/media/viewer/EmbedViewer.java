package org.metalib.papifly.fx.media.viewer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.stream.EmbedUrlResolver;

import java.io.IOException;
import java.io.OutputStream;
import java.awt.Desktop;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.Executors;

public class EmbedViewer extends StackPane {

    private static final String MODERN_USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    private static final System.Logger LOG = System.getLogger(EmbedViewer.class.getName());

    private final WebView webView = new WebView();
    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();
    private String youtubeWrapperUrl;

    public EmbedViewer(String url) {
        setMinSize(0, 0);
        webView.setContextMenuEnabled(false);
        webView.setMinSize(0, 0);
        webView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        webView.getEngine().setUserAgent(MODERN_USER_AGENT);
        wireExternalNavigation();
        getChildren().add(webView);
        wireTheme();
        load(url);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double x = snappedLeftInset();
        double y = snappedTopInset();
        double w = Math.max(0, getWidth() - x - snappedRightInset());
        double h = Math.max(0, getHeight() - y - snappedBottomInset());
        webView.resizeRelocate(x, y, w, h);
    }

    public void load(String url) {
        String embedUrl = EmbedUrlResolver.resolve(url);
        if (isYouTubeEmbed(embedUrl)) {
            youtubeWrapperUrl = LocalYouTubeWrapperServer.wrapperUrlFor(embedUrl);
            if (youtubeWrapperUrl != null) {
                webView.getEngine().load(youtubeWrapperUrl);
            } else {
                webView.getEngine().load(embedUrl);
            }
        } else {
            youtubeWrapperUrl = null;
            webView.getEngine().load(embedUrl);
        }
    }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    public void dispose() {
        webView.getEngine().load(null);
    }

    private void wireTheme() {
        themeProperty.addListener((obs, o, t) -> {
            if (t == null) return;
            setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(t.background(),
                    javafx.scene.layout.CornerRadii.EMPTY, Insets.EMPTY)));
        });
    }

    private void wireExternalNavigation() {
        webView.getEngine().locationProperty().addListener((obs, oldLocation, newLocation) -> {
            if (!shouldOpenExternally(newLocation, youtubeWrapperUrl)) return;
            openInSystemBrowser(newLocation);
            if (youtubeWrapperUrl != null) {
                Platform.runLater(() -> webView.getEngine().load(youtubeWrapperUrl));
            }
        });
    }

    private static boolean isYouTubeEmbed(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("youtube.com/embed/") || lower.contains("youtube-nocookie.com/embed/");
    }

    static boolean shouldOpenExternally(String newLocation, String youtubeWrapperUrl) {
        if (youtubeWrapperUrl == null || youtubeWrapperUrl.isBlank()) return false;
        if (newLocation == null || newLocation.isBlank()) return false;
        String lower = newLocation.toLowerCase(Locale.ROOT);
        if (lower.startsWith("about:") || lower.startsWith("data:") || lower.startsWith("javascript:")) return false;
        String wrapperOrigin = originOf(youtubeWrapperUrl);
        if (wrapperOrigin == null) return false;
        String locationOrigin = originOf(newLocation);
        if (locationOrigin == null) return true;
        return !wrapperOrigin.equals(locationOrigin);
    }

    static String wrapperUrlForTesting(String embedUrl) {
        return LocalYouTubeWrapperServer.wrapperUrlForTesting(embedUrl);
    }

    private static String originOf(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return null;
            }
            int port = uri.getPort();
            int defaultPort = defaultPort(scheme);
            if (port == -1 || port == defaultPort) {
                return scheme.toLowerCase(Locale.ROOT) + "://" + host.toLowerCase(Locale.ROOT);
            }
            return scheme.toLowerCase(Locale.ROOT) + "://" + host.toLowerCase(Locale.ROOT) + ":" + port;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static int defaultPort(String scheme) {
        return switch (scheme.toLowerCase(Locale.ROOT)) {
            case "http" -> 80;
            case "https" -> 443;
            default -> -1;
        };
    }

    private static void openInSystemBrowser(String url) {
        if (!Desktop.isDesktopSupported()) return;
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) return;
        try {
            desktop.browse(new URI(url));
        } catch (IOException | URISyntaxException e) {
            LOG.log(System.Logger.Level.WARNING, "Failed to open external browser URL: " + url, e);
        }
    }

    private static String buildYouTubeIframeHtml(String embedUrl) {
        String safeUrl = escapeHtmlAttribute(embedUrl);
        return """
            <!doctype html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="referrer" content="strict-origin-when-cross-origin">
              <style>
                html, body { margin: 0; width: 100%%; height: 100%%; overflow: hidden; background: transparent; }
                body { position: fixed; inset: 0; }
                #player { border: 0; width: 100vw; height: 100vh; display: block; }
              </style>
              <script>
                (() => {
                  const syncSize = () => {
                    const player = document.getElementById('player');
                    if (!player) return;
                    player.style.width = window.innerWidth + 'px';
                    player.style.height = window.innerHeight + 'px';
                  };
                  window.addEventListener('resize', syncSize, { passive: true });
                  if (typeof ResizeObserver !== 'undefined') {
                    new ResizeObserver(syncSize).observe(document.documentElement);
                  }
                  requestAnimationFrame(syncSize);
                })();
              </script>
            </head>
            <body>
              <iframe
                id="player"
                src="%s"
                title="YouTube video player"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                allowfullscreen
                referrerpolicy="strict-origin-when-cross-origin"></iframe>
            </body>
            </html>
            """.formatted(safeUrl);
    }

    private static String escapeHtmlAttribute(String value) {
        if (value == null) return "";
        return value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private static final class LocalYouTubeWrapperServer {
        private static final LocalYouTubeWrapperServer INSTANCE = new LocalYouTubeWrapperServer();

        private final HttpServer server;
        private final String origin;
        private final boolean available;

        private LocalYouTubeWrapperServer() {
            HttpServer createdServer = null;
            String createdOrigin = null;
            boolean createdAvailable = false;
            try {
                createdServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                createdServer.createContext("/youtube", this::handleYouTube);
                createdServer.setExecutor(Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "papiflyfx-youtube-wrapper");
                    t.setDaemon(true);
                    return t;
                }));
                createdServer.start();
                createdOrigin = "http://127.0.0.1:" + createdServer.getAddress().getPort();
                HttpServer serverToStop = createdServer;
                Runtime.getRuntime().addShutdownHook(new Thread(
                    () -> serverToStop.stop(0),
                    "papiflyfx-youtube-wrapper-stop"
                ));
                createdAvailable = true;
            } catch (IOException e) {
                LOG.log(
                    System.Logger.Level.WARNING,
                    "Local YouTube wrapper server unavailable; falling back to direct embed URLs",
                    e
                );
            }
            this.server = createdServer;
            this.origin = createdOrigin;
            this.available = createdAvailable;
        }

        private static String wrapperUrlFor(String embedUrl) {
            if (!INSTANCE.available) {
                return null;
            }
            return INSTANCE.origin + "/youtube?src=" + urlEncode(embedUrl);
        }

        private static String wrapperUrlForTesting(String embedUrl) {
            String baseOrigin = INSTANCE.available ? INSTANCE.origin : "http://127.0.0.1";
            return baseOrigin + "/youtube?src=" + urlEncode(embedUrl);
        }

        private void handleYouTube(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                write(exchange, 405, "Method Not Allowed", "text/plain; charset=UTF-8");
                return;
            }

            String embedUrl = queryParam(exchange.getRequestURI().getRawQuery(), "src");
            if (!isYouTubeEmbed(embedUrl)) {
                write(exchange, 400, "Invalid embed URL", "text/plain; charset=UTF-8");
                return;
            }

            String iframeUrl = withYouTubeContextParams(embedUrl);
            String html = buildYouTubeIframeHtml(iframeUrl);
            write(exchange, 200, html, "text/html; charset=UTF-8");
        }

        private String withYouTubeContextParams(String embedUrl) {
            String separator = embedUrl.contains("?") ? "&" : "?";
            String widgetReferrer = origin + "/youtube";
            return embedUrl + separator
                + "origin=" + urlEncode(origin)
                + "&widget_referrer=" + urlEncode(widgetReferrer);
        }

        private static String queryParam(String rawQuery, String key) {
            if (rawQuery == null || rawQuery.isBlank()) return null;
            for (String part : rawQuery.split("&")) {
                int idx = part.indexOf('=');
                String partKey = idx < 0 ? part : part.substring(0, idx);
                if (key.equals(partKey)) {
                    String value = idx < 0 ? "" : part.substring(idx + 1);
                    return URLDecoder.decode(value, StandardCharsets.UTF_8);
                }
            }
            return null;
        }

        private static String urlEncode(String value) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }

        private static void write(HttpExchange exchange, int status, String body, String contentType) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-store, max-age=0");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }
}

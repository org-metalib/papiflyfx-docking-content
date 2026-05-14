package org.metalib.papifly.fx.hugo.api;

import java.nio.file.Path;

public record HugoPreviewConfig(
    Path siteRoot,
    String contentId,
    String basePath,
    int preferredPort,
    boolean autoStart,
    boolean allowExternalNavigation
) {

    private static final int DEFAULT_PORT = 1313;

    public HugoPreviewConfig {
        if (siteRoot == null) {
            throw new IllegalArgumentException("siteRoot is required");
        }
        if (contentId == null || contentId.isBlank()) {
            throw new IllegalArgumentException("contentId is required");
        }
        siteRoot = siteRoot.toAbsolutePath().normalize();
        basePath = normalizeBasePath(basePath);
        preferredPort = preferredPort > 0 ? preferredPort : DEFAULT_PORT;
    }

    private static String normalizeBasePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }
}

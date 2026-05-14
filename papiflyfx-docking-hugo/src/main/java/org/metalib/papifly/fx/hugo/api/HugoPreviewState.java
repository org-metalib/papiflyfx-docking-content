package org.metalib.papifly.fx.hugo.api;

public record HugoPreviewState(
    String siteDir,
    String relativePath,
    boolean drafts
) {
}

package org.metalib.papifly.fx.github.model;

import java.util.Objects;

public record PullRequestDraft(
    String title,
    String body,
    String head,
    String base,
    boolean openInBrowser
) {

    public PullRequestDraft {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(head, "head");
        Objects.requireNonNull(base, "base");
    }

    public PullRequestDraft withDefaults(String fallbackTitle, String fallbackHead, String fallbackBase) {
        String normalizedTitle = title.isBlank() ? fallbackTitle : title;
        String normalizedHead = head.isBlank() ? fallbackHead : head;
        String normalizedBase = base.isBlank() ? fallbackBase : base;
        String normalizedBody = body == null ? "" : body;
        return new PullRequestDraft(normalizedTitle, normalizedBody, normalizedHead, normalizedBase, openInBrowser);
    }
}

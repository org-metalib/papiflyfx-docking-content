package org.metalib.papifly.fx.github.github;

public class GitHubApiException extends RuntimeException {

    public enum Category {
        AUTHENTICATION,
        VALIDATION,
        TRANSIENT,
        NETWORK,
        UNEXPECTED
    }

    private final Category category;
    private final int statusCode;

    public GitHubApiException(Category category, String message, int statusCode) {
        super(message);
        this.category = category;
        this.statusCode = statusCode;
    }

    public GitHubApiException(Category category, String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.category = category;
        this.statusCode = statusCode;
    }

    public Category category() {
        return category;
    }

    public int statusCode() {
        return statusCode;
    }
}

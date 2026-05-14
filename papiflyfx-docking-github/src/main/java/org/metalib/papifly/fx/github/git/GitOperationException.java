package org.metalib.papifly.fx.github.git;

public class GitOperationException extends RuntimeException {

    public GitOperationException(String message) {
        super(message);
    }

    public GitOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

package io.github.abhi07799.platform.administration.application;

public class SessionRevocationUnavailableException extends RuntimeException {

    public SessionRevocationUnavailableException(Throwable cause) {
        super("The session revocation operation could not acquire its application coordination lock.", cause);
    }
}

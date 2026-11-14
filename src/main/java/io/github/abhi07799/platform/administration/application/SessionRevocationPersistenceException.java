package io.github.abhi07799.platform.administration.application;

public class SessionRevocationPersistenceException extends RuntimeException {

    public SessionRevocationPersistenceException(Throwable cause) {
        super("Keycloak sessions were terminated, but local session revocation could not be persisted.", cause);
    }
}

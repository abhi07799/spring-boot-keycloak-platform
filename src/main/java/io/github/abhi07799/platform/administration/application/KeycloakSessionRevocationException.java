package io.github.abhi07799.platform.administration.application;

public class KeycloakSessionRevocationException extends RuntimeException {

    public KeycloakSessionRevocationException(Throwable cause) {
        super("The identity provider could not terminate the user's sessions.", cause);
    }
}

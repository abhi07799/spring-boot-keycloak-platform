package io.github.abhi07799.platform.administration.application;

public class KeycloakAccountStatusUpdateException extends RuntimeException {

    public KeycloakAccountStatusUpdateException(Throwable cause) {
        super("The identity provider could not update the account status.", cause);
    }
}

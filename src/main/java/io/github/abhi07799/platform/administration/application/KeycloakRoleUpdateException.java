package io.github.abhi07799.platform.administration.application;

public class KeycloakRoleUpdateException extends RuntimeException {
    public KeycloakRoleUpdateException(Throwable cause) {
        super("The identity provider could not update the account role.", cause);
    }
}

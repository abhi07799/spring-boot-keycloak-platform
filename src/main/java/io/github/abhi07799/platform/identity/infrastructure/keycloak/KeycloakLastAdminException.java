package io.github.abhi07799.platform.identity.infrastructure.keycloak;

public class KeycloakLastAdminException extends RuntimeException {
    public KeycloakLastAdminException() {
        super("The last application administrator cannot be demoted.");
    }
}

package io.github.abhi07799.platform.identity.infrastructure.keycloak;

import java.util.List;

public record KeycloakUserRepresentation(
        String username,
        String email,
        String firstName,
        String lastName,
        Boolean enabled,
        Boolean emailVerified,
        List<String> requiredActions,
        List<CredentialRepresentation> credentials) {

    public record CredentialRepresentation(
            String type,
            String value,
            boolean temporary) {
    }
}

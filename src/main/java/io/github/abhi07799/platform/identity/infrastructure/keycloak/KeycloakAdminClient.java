package io.github.abhi07799.platform.identity.infrastructure.keycloak;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);
    private static final List<String> PLATFORM_ROLES = List.of("USER", "STAFF", "ADMIN");

    private final KeycloakAdminTokenProvider tokenProvider;
    private final RestClient restClient;
    private final String realm;

    public KeycloakAdminClient(
            KeycloakAdminTokenProvider tokenProvider,
            RestClient.Builder restClientBuilder,
            @Value("${keycloak.admin.base-url}") String baseUrl,
            @Value("${keycloak.admin.realm}") String realm) {
        this.tokenProvider = tokenProvider;
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.realm = realm;
    }

    public String createUser(KeycloakUserRepresentation user) {
        ResponseEntity<Void> response = restClient.post()
                .uri("/admin/realms/{realm}/users", realm)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(user)
                .retrieve()
                .toBodilessEntity();

        URI location = response.getHeaders().getLocation();
        if (location == null || location.getPath() == null) {
            throw new IllegalStateException("Keycloak created the user but did not return a user Location header");
        }

        String path = location.getPath();
        String userId = path.substring(path.lastIndexOf('/') + 1);
        if (userId.isBlank()) {
            throw new IllegalStateException("Keycloak returned an invalid user Location header");
        }
        return userId;
    }

    public boolean updateUserEnabled(String userId, boolean enabled) {
        Map<String, Object> user = restClient.get()
                .uri("/admin/realms/{realm}/users/{userId}", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });

        if (user == null || user.isEmpty()) {
            throw new IllegalStateException("Keycloak user representation was not found: " + userId);
        }

        Object enabledValue = user.get("enabled");
        if (!(enabledValue instanceof Boolean previousEnabled)) {
            throw new IllegalStateException("Keycloak user representation did not include an enabled flag: " + userId);
        }

        user.put("enabled", enabled);
        restClient.put()
                .uri("/admin/realms/{realm}/users/{userId}", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(user)
                .retrieve()
                .toBodilessEntity();
        return previousEnabled;
    }

    public void deleteUser(String userId) {
        restClient.delete()
                .uri("/admin/realms/{realm}/users/{userId}", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .retrieve()
                .toBodilessEntity();
    }

    public void assignClientRole(String userId, String clientId, String roleName) {
        String clientUuid = resolveClientUuid(clientId);
        KeycloakRoleRepresentation role = getClientRole(clientUuid, roleName);
        addClientRoleMapping(userId, clientUuid, role);
    }

    /**
     * Replaces the user's single platform-api application role. Other client and realm
     * role mappings are deliberately left untouched.
     *
     * @return the previous platform-api role
     */
    public String updateClientRole(String userId, String clientId, String targetRole) {
        return replaceClientRole(userId, clientId, targetRole, true);
    }

    /** Restores a previously known role after a failed application-side transaction. */
    public String restoreClientRole(String userId, String clientId, String targetRole) {
        return replaceClientRole(userId, clientId, targetRole, false);
    }

    private String replaceClientRole(String userId, String clientId, String targetRole, boolean protectLastAdmin) {
        if (!PLATFORM_ROLES.contains(targetRole)) {
            throw new IllegalArgumentException("Unsupported platform role: " + targetRole);
        }

        String clientUuid = resolveClientUuid(clientId);
        List<KeycloakRoleRepresentation> mappings = getClientRoleMappings(userId, clientUuid);
        List<KeycloakRoleRepresentation> currentPlatformRoles = mappings.stream()
                .filter(role -> PLATFORM_ROLES.contains(role.name()))
                .toList();

        if (currentPlatformRoles.size() != 1) {
            throw new IllegalStateException("Expected exactly one platform-api role for Keycloak user " + userId);
        }

        KeycloakRoleRepresentation previousRole = currentPlatformRoles.getFirst();
        if (previousRole.name().equals(targetRole)) {
            return previousRole.name();
        }

        if (protectLastAdmin && "ADMIN".equals(previousRole.name()) && !"ADMIN".equals(targetRole)) {
            if (!hasAtLeastTwoEnabledAdministrators(clientUuid)) {
                throw new KeycloakLastAdminException();
            }
        }

        KeycloakRoleRepresentation requestedRole = getClientRole(clientUuid, targetRole);
        addClientRoleMapping(userId, clientUuid, requestedRole);
        try {
            removeClientRoleMapping(userId, clientUuid, previousRole);
        } catch (RuntimeException removalFailure) {
            try {
                removeClientRoleMapping(userId, clientUuid, requestedRole);
            } catch (RuntimeException compensationFailure) {
                log.error("Could not remove newly assigned platform role {} after failing to remove {} for Keycloak user {}",
                        targetRole, previousRole.name(), userId, compensationFailure);
                removalFailure.addSuppressed(compensationFailure);
            }
            throw removalFailure;
        }
        return previousRole.name();
    }

    private String resolveClientUuid(String clientId) {
        List<KeycloakClientReference> clients = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/clients")
                        .queryParam("clientId", clientId)
                        .build(realm))
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });

        if (clients == null || clients.size() != 1 || clients.getFirst().id() == null) {
            throw new IllegalStateException("Expected exactly one Keycloak client with ID: " + clientId);
        }
        return clients.getFirst().id();
    }

    private KeycloakRoleRepresentation getClientRole(String clientUuid, String roleName) {
        KeycloakRoleRepresentation role = restClient.get()
                .uri("/admin/realms/{realm}/clients/{clientUuid}/roles/{roleName}",
                        realm, clientUuid, roleName)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .retrieve()
                .body(KeycloakRoleRepresentation.class);

        if (role == null || role.id() == null) {
            throw new IllegalStateException("Keycloak client role was not found: " + roleName);
        }
        return role;
    }

    private List<KeycloakRoleRepresentation> getClientRoleMappings(String userId, String clientUuid) {
        List<KeycloakRoleRepresentation> roles = restClient.get()
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/clients/{clientUuid}",
                        realm, userId, clientUuid)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });
        return roles == null ? List.of() : roles;
    }

    private boolean hasAtLeastTwoEnabledAdministrators(String clientUuid) {
        int first = 0;
        int enabledCount = 0;
        int pageSize = 100;
        while (true) {
            List<Map<String, Object>> users = getClientRoleUsers(clientUuid, "ADMIN", first, pageSize);
            if (users == null || users.isEmpty()) {
                return false;
            }
            enabledCount += (int) users.stream().filter(user -> Boolean.TRUE.equals(user.get("enabled"))).count();
            if (enabledCount >= 2) {
                return true;
            }
            if (users.size() < pageSize) {
                return false;
            }
            first += users.size();
        }
    }

    private List<Map<String, Object>> getClientRoleUsers(String clientUuid, String roleName, int first, int max) {
        List<Map<String, Object>> users = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/clients/{clientUuid}/roles/{roleName}/users")
                        .queryParam("first", first)
                        .queryParam("max", max)
                        .build(realm, clientUuid, roleName))
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });
        return users;
    }

    private void addClientRoleMapping(
            String userId, String clientUuid, KeycloakRoleRepresentation role) {
        restClient.post()
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/clients/{clientUuid}",
                        realm, userId, clientUuid)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
    }

    private void removeClientRoleMapping(
            String userId, String clientUuid, KeycloakRoleRepresentation role) {
        restClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/clients/{clientUuid}",
                        realm, userId, clientUuid)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
    }

    private String bearerToken() {
        return "Bearer " + tokenProvider.getAccessToken();
    }

    private record KeycloakClientReference(String id) {
    }

    private record KeycloakRoleRepresentation(
            String id,
            String name,
            Boolean composite,
            Boolean clientRole,
            String containerId) {
    }
}

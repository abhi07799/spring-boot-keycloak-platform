package io.github.abhi07799.platform.identity.infrastructure.keycloak;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KeycloakAdminClient {

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

    public void updateUserEnabled(String userId, boolean enabled) {
        Map<String, Object> user = restClient.get()
                .uri("/admin/realms/{realm}/users/{userId}", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });

        if (user == null || user.isEmpty()) {
            throw new IllegalStateException("Keycloak user representation was not found: " + userId);
        }

        user.put("enabled", enabled);
        restClient.put()
                .uri("/admin/realms/{realm}/users/{userId}", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(user)
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteUser(String userId) {
        restClient.delete()
                .uri("/admin/realms/{realm}/users/{userId}", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .retrieve()
                .toBodilessEntity();
    }

    public void assignClientRole(String userId, String clientId, String roleName) {
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

        String clientUuid = clients.getFirst().id();
        KeycloakRoleRepresentation role = restClient.get()
                .uri("/admin/realms/{realm}/clients/{clientUuid}/roles/{roleName}",
                        realm, clientUuid, roleName)
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .retrieve()
                .body(KeycloakRoleRepresentation.class);

        if (role == null || role.id() == null) {
            throw new IllegalStateException("Keycloak client role was not found: " + clientId + "/" + roleName);
        }

        restClient.post()
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

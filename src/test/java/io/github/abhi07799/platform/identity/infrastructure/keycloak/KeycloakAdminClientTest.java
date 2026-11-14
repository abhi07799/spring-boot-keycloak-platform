package io.github.abhi07799.platform.identity.infrastructure.keycloak;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakAdminClientTest {

    private MockRestServiceServer server;
    private KeycloakAdminTokenProvider tokenProvider;
    private KeycloakAdminClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        tokenProvider = mock(KeycloakAdminTokenProvider.class);
        when(tokenProvider.getAccessToken()).thenReturn("service-token");
        client = new KeycloakAdminClient(tokenProvider, builder, "http://localhost:8081", "platform");
    }

    @Test
    void createsUserAndReturnsIdFromLocationHeader() {
        server.expect(requestTo("http://localhost:8081/admin/realms/platform/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer service-token"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .header("Location", "http://localhost:8081/admin/realms/platform/users/user-123"));

        KeycloakUserRepresentation user = new KeycloakUserRepresentation(
                "person@example.com", "person@example.com", "Test", "Person",
                true, false, List.of(),
                List.of(new KeycloakUserRepresentation.CredentialRepresentation(
                        "password", "not-a-real-password", true)));

        assertThat(client.createUser(user)).isEqualTo("user-123");
        server.verify();
    }

    @Test
    void deletesUserForRegistrationCompensation() {
        server.expect(requestTo("http://localhost:8081/admin/realms/platform/users/user-123"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Authorization", "Bearer service-token"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.deleteUser("user-123");
        server.verify();
    }

    @Test
    void assignsClientRoleUsingResolvedClientUuidAndRoleRepresentation() {
        server.expect(requestTo("http://localhost:8081/admin/realms/platform/clients?clientId=platform-api"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer service-token"))
                .andRespond(withSuccess("[{\"id\":\"client-uuid\"}]", MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://localhost:8081/admin/realms/platform/clients/client-uuid/roles/USER"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"id\":\"role-uuid\",\"name\":\"USER\",\"composite\":false,\"clientRole\":true,\"containerId\":\"client-uuid\"}",
                        MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://localhost:8081/admin/realms/platform/users/user-123/role-mappings/clients/client-uuid"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("[{\"id\":\"role-uuid\",\"name\":\"USER\",\"composite\":false,\"clientRole\":true,\"containerId\":\"client-uuid\"}]"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.assignClientRole("user-123", "platform-api", "USER");
        server.verify();
    }

    @Test
    void updatesOnlyTheEnabledFlagAndPreservesExistingKeycloakUserFields() {
        server.expect(requestTo("http://localhost:8081/admin/realms/platform/users/user-123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer service-token"))
                .andRespond(withSuccess(
                        "{\\"id\\":\\"user-123\\",\\"username\\":\\"person@example.com\\",\\"email\\":\\"person@example.com\\",\\"enabled\\":true}",
                        MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://localhost:8081/admin/realms/platform/users/user-123"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Authorization", "Bearer service-token"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"id":"user-123","username":"person@example.com","email":"person@example.com","enabled":false}
                        """))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.updateUserEnabled("user-123", false);
        server.verify();
    }

}

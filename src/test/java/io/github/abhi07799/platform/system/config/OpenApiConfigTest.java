package io.github.abhi07799.platform.system.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void exposesKeycloakAuthorizationCodeEndpointsAndOpenIdScope() {
        OpenAPI openApi = config.platformOpenApi("http://localhost:8081/realms/platform");

        var scheme = openApi.getComponents().getSecuritySchemes().get(OpenApiConfig.OAUTH_SCHEME_NAME);
        var flow = scheme.getFlows().getAuthorizationCode();

        assertThat(scheme.getType()).isEqualTo(io.swagger.v3.oas.models.security.SecurityScheme.Type.OAUTH2);
        assertThat(flow.getAuthorizationUrl())
                .isEqualTo("http://localhost:8081/realms/platform/protocol/openid-connect/auth");
        assertThat(flow.getTokenUrl())
                .isEqualTo("http://localhost:8081/realms/platform/protocol/openid-connect/token");
        assertThat(flow.getScopes()).containsKey("openid");
    }

    @Test
    void removesTrailingSlashFromIssuerBeforeBuildingEndpoints() {
        OpenAPI openApi = config.platformOpenApi("http://localhost:8081/realms/platform/");

        assertThat(openApi.getComponents().getSecuritySchemes()
                .get(OpenApiConfig.OAUTH_SCHEME_NAME).getFlows().getAuthorizationCode().getTokenUrl())
                .isEqualTo("http://localhost:8081/realms/platform/protocol/openid-connect/token");
    }
}

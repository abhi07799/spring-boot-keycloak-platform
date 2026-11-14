package io.github.abhi07799.platform.system.config;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static final String OAUTH_SCHEME_NAME = "keycloakOAuth2";

    @Bean
    OpenAPI platformOpenApi(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {

        String issuer = issuerUri.replaceAll("/+$", "");
        OAuthFlow authorizationCodeFlow = new OAuthFlow()
                .authorizationUrl(issuer + "/protocol/openid-connect/auth")
                .tokenUrl(issuer + "/protocol/openid-connect/token")
                .scopes(new Scopes().addString("openid", "OpenID Connect"));

        SecurityScheme oauthScheme = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows().authorizationCode(authorizationCodeFlow));

        return new OpenAPI()
                .components(new Components().addSecuritySchemes(OAUTH_SCHEME_NAME, oauthScheme))
                .addSecurityItem(new SecurityRequirement().addList(OAUTH_SCHEME_NAME, List.of("openid")));
    }
}

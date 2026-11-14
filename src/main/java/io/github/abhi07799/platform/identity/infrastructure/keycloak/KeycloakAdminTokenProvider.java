package io.github.abhi07799.platform.identity.infrastructure.keycloak;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

@Component
public class KeycloakAdminTokenProvider {

    private static final String CLIENT_REGISTRATION_ID = "keycloak-admin";
    private static final String SERVICE_PRINCIPAL = "platform-admin-service";

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public KeycloakAdminTokenProvider(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    public String getAccessToken() {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId(CLIENT_REGISTRATION_ID)
                .principal(SERVICE_PRINCIPAL)
                .build();

        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(request);
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new IllegalStateException("Keycloak admin service account could not obtain an access token");
        }

        return authorizedClient.getAccessToken().getTokenValue();
    }
}

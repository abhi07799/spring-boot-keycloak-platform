package io.github.abhi07799.platform.identity.infrastructure.keycloak;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeycloakAdminTokenProviderTest {

    @Test
    void returnsAccessTokenFromAuthorizedClientManager() {
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(manager.authorize(any())).thenReturn(authorizedClient);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn("test-access-token");

        KeycloakAdminTokenProvider provider = new KeycloakAdminTokenProvider(manager);

        assertThat(provider.getAccessToken()).isEqualTo("test-access-token");
    }

    @Test
    void failsClearlyWhenAuthorizedClientCannotBeObtained() {
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        when(manager.authorize(any())).thenReturn(null);

        KeycloakAdminTokenProvider provider = new KeycloakAdminTokenProvider(manager);

        assertThatThrownBy(provider::getAccessToken)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("could not obtain an access token");
    }
}

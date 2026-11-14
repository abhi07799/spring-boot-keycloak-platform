package io.github.abhi07799.platform.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRoleConverterTest {

    private final KeycloakRoleConverter converter = new KeycloakRoleConverter();

    @Test
    void mapsOnlyKnownPlatformClientRoles() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("resource_access", Map.of(
                        "platform-api", Map.of("roles", List.of("ADMIN", "STAFF", "USER", "UNKNOWN")),
                        "another-client", Map.of("roles", List.of("ADMIN"))))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER");
    }

    @Test
    void returnsNoAuthoritiesWhenPlatformClientRolesAreMissing() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("resource_access", Map.of("another-client", Map.of("roles", List.of("ADMIN"))))
                .build();

        assertThat(converter.convert(jwt)).isEmpty();
    }
}

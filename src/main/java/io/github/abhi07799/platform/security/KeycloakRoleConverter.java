package io.github.abhi07799.platform.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String API_CLIENT_ID = "platform-api";
    private static final Set<String> SUPPORTED_ROLES = Set.of("ADMIN", "STAFF", "USER");

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) {
            return List.of();
        }

        Object apiClientAccess = resourceAccess.get(API_CLIENT_ID);
        if (!(apiClientAccess instanceof Map<?, ?> clientAccess)) {
            return List.of();
        }

        Object rolesClaim = clientAccess.get("roles");
        if (!(rolesClaim instanceof Collection<?> roles)) {
            return List.of();
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Object role : roles) {
            if (role instanceof String roleName && SUPPORTED_ROLES.contains(roleName)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
            }
        }
        return List.copyOf(authorities);
    }
}

package io.github.abhi07799.platform.security.config;

import io.github.abhi07799.platform.security.AccountStatusJwtValidator;
import io.github.abhi07799.platform.security.KeycloakRoleConverter;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final String API_AUDIENCE = "platform-api";

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/v1/ping",
                                "/api/v1/auth/register"
                        ).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            UserProfileRepository userProfileRepository) {
        String normalizedIssuer = issuerUri.replaceAll("/+$", "");
        String jwkSetUri = normalizedIssuer + "/protocol/openid-connect/certs";

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> standardValidator = JwtValidators.createDefaultWithIssuer(normalizedIssuer);
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(API_AUDIENCE)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Required audience platform-api is missing", null));
        OAuth2TokenValidator<Jwt> accountStatusValidator = new AccountStatusJwtValidator(userProfileRepository);

        OAuth2TokenValidator<Jwt> platformTokenValidator = jwt -> {
            OAuth2TokenValidatorResult standardResult = standardValidator.validate(jwt);
            if (!standardResult.getErrors().isEmpty()) {
                return standardResult;
            }

            OAuth2TokenValidatorResult audienceResult = audienceValidator.validate(jwt);
            if (!audienceResult.getErrors().isEmpty()) {
                return audienceResult;
            }

            return accountStatusValidator.validate(jwt);
        };
        decoder.setJwtValidator(platformTokenValidator);
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(KeycloakRoleConverter keycloakRoleConverter) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(keycloakRoleConverter);
        return converter;
    }
}

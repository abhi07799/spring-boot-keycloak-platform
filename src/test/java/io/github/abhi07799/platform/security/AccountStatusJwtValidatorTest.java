package io.github.abhi07799.platform.security;

import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountStatusJwtValidatorTest {

    private static final String SUBJECT = "keycloak-subject-123";

    @Mock
    private UserProfileRepository userProfileRepository;

    private AccountStatusJwtValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AccountStatusJwtValidator(userProfileRepository);
    }

    @Test
    void acceptsTokenForEnabledApplicationAccount() {
        UserProfile profile = new UserProfile(
                SUBJECT, "Test Person", "person@example.com", "+919876543210");
        when(userProfileRepository.findByKeycloakSubjectId(SUBJECT)).thenReturn(Optional.of(profile));

        OAuth2TokenValidatorResult result = validator.validate(jwt(SUBJECT));

        assertThat(result.getErrors()).isEmpty();
        verify(userProfileRepository).findByKeycloakSubjectId(SUBJECT);
    }

    @Test
    void rejectsTokenForDisabledApplicationAccount() {
        UserProfile profile = new UserProfile(
                SUBJECT, "Test Person", "person@example.com", "+919876543210");
        profile.updateEnabled(false);
        when(userProfileRepository.findByKeycloakSubjectId(SUBJECT)).thenReturn(Optional.of(profile));

        OAuth2TokenValidatorResult result = validator.validate(jwt(SUBJECT));

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().iterator().next().getErrorCode()).isEqualTo("invalid_token");
    }

    @Test
    void rejectsTokenWhenApplicationProfileDoesNotExist() {
        when(userProfileRepository.findByKeycloakSubjectId(SUBJECT)).thenReturn(Optional.empty());

        OAuth2TokenValidatorResult result = validator.validate(jwt(SUBJECT));

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().iterator().next().getErrorCode()).isEqualTo("invalid_token");
    }

    @Test
    void rejectsTokenWithoutSubjectWithoutQueryingDatabase() {
        OAuth2TokenValidatorResult result = validator.validate(jwt(null));

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().iterator().next().getErrorCode()).isEqualTo("invalid_token");
    }

    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(subject)
                .claim("iss", "http://localhost:8081/realms/platform")
                .build();
    }
}

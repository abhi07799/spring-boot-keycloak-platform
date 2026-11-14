package io.github.abhi07799.platform.security;

import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.time.Instant;

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


    @Test
    void rechecksAccountStatusOnEveryTokenValidation() {
        UserProfile enabledProfile = new UserProfile(
                SUBJECT, "Test Person", "person@example.com", "+919876543210");
        UserProfile disabledProfile = new UserProfile(
                SUBJECT, "Test Person", "person@example.com", "+919876543210");
        disabledProfile.updateEnabled(false);
        when(userProfileRepository.findByKeycloakSubjectId(SUBJECT))
                .thenReturn(Optional.of(enabledProfile), Optional.of(disabledProfile));

        OAuth2TokenValidatorResult firstRequest = validator.validate(jwt(SUBJECT));
        OAuth2TokenValidatorResult secondRequest = validator.validate(jwt(SUBJECT));

        assertThat(firstRequest.getErrors()).isEmpty();
        assertThat(secondRequest.getErrors()).hasSize(1);
        verify(userProfileRepository, org.mockito.Mockito.times(2))
                .findByKeycloakSubjectId(SUBJECT);
    }


    @Test
    void doesNotAcceptTokenWhenAccountStatusCannotBeChecked() {
        when(userProfileRepository.findByKeycloakSubjectId(SUBJECT))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> validator.validate(jwt(SUBJECT)))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    private Jwt jwt(String subject) {
        return jwt(subject, Instant.now().plusSeconds(2));
    }


    @Test
    void rejectsTokenIssuedBeforeTheLatestRoleOrStatusChange() {
        UserProfile profile = new UserProfile(
                SUBJECT, "Test Person", "person@example.com", "+919876543210");
        profile.markAuthorizationUpdated();
        when(userProfileRepository.findByKeycloakSubjectId(SUBJECT)).thenReturn(Optional.of(profile));

        OAuth2TokenValidatorResult result = validator.validate(jwt(SUBJECT, Instant.now().minusSeconds(2)));

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().iterator().next().getErrorCode()).isEqualTo("invalid_token");
    }

    @Test
    void acceptsTokenIssuedAfterTheLatestRoleOrStatusChange() {
        UserProfile profile = new UserProfile(
                SUBJECT, "Test Person", "person@example.com", "+919876543210");
        profile.markAuthorizationUpdated();
        when(userProfileRepository.findByKeycloakSubjectId(SUBJECT)).thenReturn(Optional.of(profile));

        OAuth2TokenValidatorResult result = validator.validate(jwt(SUBJECT, Instant.now().plusSeconds(2)));

        assertThat(result.getErrors()).isEmpty();
    }

    private Jwt jwt(String subject, Instant issuedAt) {
        var builder = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("iss", "http://localhost:8081/realms/platform")
                .issuedAt(issuedAt);
        if (subject != null) {
            builder.subject(subject);
        }
        return builder.build();
    }
}

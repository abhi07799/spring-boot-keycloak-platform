package io.github.abhi07799.platform.identity.application;

import io.github.abhi07799.platform.identity.infrastructure.keycloak.KeycloakAdminClient;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountRecoveryServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    private AccountRecoveryService service;

    @BeforeEach
    void setUp() {
        service = new AccountRecoveryService(userProfileRepository, keycloakAdminClient);
    }

    @Test
    void requestsPasswordResetForNormalizedEnabledEmail() {
        UserProfile profile = profile(true);
        when(userProfileRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(profile));

        service.requestPasswordReset(" Person@Example.com ");

        verify(keycloakAdminClient).sendPasswordResetEmail("keycloak-subject");
    }

    @Test
    void doesNothingForUnknownOrDisabledAccounts() {
        UserProfile disabledProfile = profile(false);
        when(userProfileRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        when(userProfileRepository.findByEmailIgnoreCase("disabled@example.com"))
                .thenReturn(Optional.of(disabledProfile));

        service.requestPasswordReset("missing@example.com");
        service.requestPasswordReset("disabled@example.com");
        service.resendEmailVerification("missing@example.com");
        service.resendEmailVerification("disabled@example.com");

        verify(keycloakAdminClient, never()).sendPasswordResetEmail(org.mockito.ArgumentMatchers.any());
        verify(keycloakAdminClient, never()).sendVerifyEmail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void resendsVerificationOnlyWhenEmailIsNotAlreadyVerified() {
        UserProfile profile = profile(true);
        when(userProfileRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(profile));
        when(keycloakAdminClient.isEmailVerified("keycloak-subject")).thenReturn(false);

        service.resendEmailVerification("person@example.com");

        verify(keycloakAdminClient).sendVerifyEmail("keycloak-subject");
    }

    @Test
    void doesNotSendAnotherVerificationForVerifiedAddress() {
        UserProfile profile = profile(true);
        when(userProfileRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(profile));
        when(keycloakAdminClient.isEmailVerified("keycloak-subject")).thenReturn(true);

        service.resendEmailVerification("person@example.com");

        verify(keycloakAdminClient, never()).sendVerifyEmail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void suppressesProviderFailuresToKeepPublicResponseGeneric() {
        UserProfile profile = profile(true);
        when(userProfileRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(profile));
        org.mockito.Mockito.doThrow(new IllegalStateException("provider unavailable"))
                .when(keycloakAdminClient).sendPasswordResetEmail("keycloak-subject");

        service.requestPasswordReset("person@example.com");
    }

    private UserProfile profile(boolean enabled) {
        UserProfile profile = org.mockito.Mockito.mock(UserProfile.class);
        lenient().when(profile.getId()).thenReturn(UUID.fromString("b0e6d5a1-7d25-4a6e-8f44-19e27a5a1001"));
        lenient().when(profile.getKeycloakSubjectId()).thenReturn("keycloak-subject");
        when(profile.isEnabled()).thenReturn(enabled);
        return profile;
    }
}

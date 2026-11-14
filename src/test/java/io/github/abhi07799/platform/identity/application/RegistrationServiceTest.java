package io.github.abhi07799.platform.identity.application;

import io.github.abhi07799.platform.identity.api.RegistrationRequest;
import io.github.abhi07799.platform.identity.api.RegistrationResponse;
import io.github.abhi07799.platform.identity.infrastructure.keycloak.KeycloakAdminClient;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @Mock
    private UserProfileRepository userProfileRepository;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(keycloakAdminClient, userProfileRepository);
    }

    @Test
    void registersUserWithOnlyTheDefaultUserRoleAndPersistsProfile() {
        when(userProfileRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(keycloakAdminClient.createUser(any())).thenReturn("keycloak-user-123");
        when(userProfileRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationResponse response = registrationService.register(validRequest());

        assertThat(response.fullName()).isEqualTo("Test Person");
        assertThat(response.email()).isEqualTo("person@example.com");

        InOrder order = inOrder(keycloakAdminClient, userProfileRepository);
        order.verify(keycloakAdminClient).createUser(any());
        order.verify(keycloakAdminClient).assignClientRole("keycloak-user-123", "platform-api", "USER");
        order.verify(userProfileRepository).saveAndFlush(any());
        order.verify(keycloakAdminClient).sendVerifyEmail("keycloak-user-123");
        verify(keycloakAdminClient, never()).deleteUser(any());
    }

    @Test
    void keepsRegistrationSuccessfulWhenVerificationEmailDeliveryFails() {
        when(userProfileRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(keycloakAdminClient.createUser(any())).thenReturn("keycloak-user-123");
        when(userProfileRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(new IllegalStateException("SMTP unavailable"))
                .when(keycloakAdminClient).sendVerifyEmail("keycloak-user-123");

        RegistrationResponse response = registrationService.register(validRequest());

        assertThat(response.email()).isEqualTo("person@example.com");
        verify(userProfileRepository).saveAndFlush(any());
        verify(keycloakAdminClient, never()).deleteUser(any());
    }

    @Test
    void rejectsExistingEmailBeforeCallingKeycloak() {
        when(userProfileRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(validRequest()))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(keycloakAdminClient, never()).createUser(any());
    }

    @Test
    void deletesKeycloakUserWhenProfilePersistenceFails() {
        when(userProfileRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(keycloakAdminClient.createUser(any())).thenReturn("keycloak-user-123");
        when(userProfileRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("unique email"));

        assertThatThrownBy(() -> registrationService.register(validRequest()))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(keycloakAdminClient).deleteUser("keycloak-user-123");
    }

    @Test
    void deletesKeycloakUserWhenRoleAssignmentFails() {
        when(userProfileRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(keycloakAdminClient.createUser(any())).thenReturn("keycloak-user-123");
        org.mockito.Mockito.doThrow(new IllegalStateException("role assignment failed"))
                .when(keycloakAdminClient).assignClientRole("keycloak-user-123", "platform-api", "USER");

        assertThatThrownBy(() -> registrationService.register(validRequest()))
                .isInstanceOf(RegistrationFailedException.class);

        verify(keycloakAdminClient).deleteUser("keycloak-user-123");
        verify(userProfileRepository, never()).saveAndFlush(any());
    }

    @Test
    void reportsWhenCompensationAlsoFails() {
        when(userProfileRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(keycloakAdminClient.createUser(any())).thenReturn("keycloak-user-123");
        org.mockito.Mockito.doThrow(new IllegalStateException("role assignment failed"))
                .when(keycloakAdminClient).assignClientRole("keycloak-user-123", "platform-api", "USER");
        org.mockito.Mockito.doThrow(new IllegalStateException("delete failed"))
                .when(keycloakAdminClient).deleteUser("keycloak-user-123");

        assertThatThrownBy(() -> registrationService.register(validRequest()))
                .isInstanceOf(RegistrationCompensationException.class);
    }

    private RegistrationRequest validRequest() {
        return new RegistrationRequest(
                " Test Person ", " Person@Example.com ", "+919876543210", "a-long-test-password");
    }
}

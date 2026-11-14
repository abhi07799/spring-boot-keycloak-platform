package io.github.abhi07799.platform.administration.application;

import io.github.abhi07799.platform.administration.api.AdminUserProvisioningRequest;
import io.github.abhi07799.platform.identity.api.RegistrationResponse;
import io.github.abhi07799.platform.identity.application.EmailAlreadyRegisteredException;
import io.github.abhi07799.platform.identity.application.RegistrationCompensationException;
import io.github.abhi07799.platform.identity.application.RegistrationFailedException;
import io.github.abhi07799.platform.identity.infrastructure.keycloak.KeycloakAdminClient;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserProvisioningServiceTest {

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @Mock
    private UserProfileRepository userProfileRepository;

    private AdminUserProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserProvisioningService(keycloakAdminClient, userProfileRepository);
    }

    @Test
    void provisionsOnlyTheRequestedPrivilegedRoleAndPersistsProfile() {
        when(userProfileRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(keycloakAdminClient.createUser(any())).thenReturn("keycloak-user-123");
        when(userProfileRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationResponse response = service.provision(request(AdminUserProvisioningRequest.ProvisionedRole.STAFF));

        assertThat(response.email()).isEqualTo("person@example.com");
        InOrder order = inOrder(keycloakAdminClient, userProfileRepository);
        order.verify(keycloakAdminClient).createUser(any());
        order.verify(keycloakAdminClient).assignClientRole("keycloak-user-123", "platform-api", "STAFF");
        order.verify(userProfileRepository).saveAndFlush(any());
        verify(keycloakAdminClient, never()).deleteUser(any());
    }

    @Test
    void canProvisionAdminRoleWhenExplicitlyRequested() {
        when(userProfileRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(keycloakAdminClient.createUser(any())).thenReturn("keycloak-user-123");
        when(userProfileRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.provision(request(AdminUserProvisioningRequest.ProvisionedRole.ADMIN));

        verify(keycloakAdminClient).assignClientRole("keycloak-user-123", "platform-api", "ADMIN");
    }

    @Test
    void rejectsDuplicateEmailBeforeCallingKeycloak() {
        when(userProfileRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.provision(request(AdminUserProvisioningRequest.ProvisionedRole.STAFF)))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(keycloakAdminClient, never()).createUser(any());
    }

    @Test
    void compensatesWhenRoleAssignmentFails() {
        when(userProfileRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(keycloakAdminClient.createUser(any())).thenReturn("keycloak-user-123");
        doThrow(new IllegalStateException("role assignment failed"))
                .when(keycloakAdminClient).assignClientRole("keycloak-user-123", "platform-api", "STAFF");

        assertThatThrownBy(() -> service.provision(request(AdminUserProvisioningRequest.ProvisionedRole.STAFF)))
                .isInstanceOf(RegistrationFailedException.class);
        verify(keycloakAdminClient).deleteUser("keycloak-user-123");
        verify(userProfileRepository, never()).saveAndFlush(any());
    }

    @Test
    void compensatesWhenProfilePersistenceFails() {
        when(userProfileRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(keycloakAdminClient.createUser(any())).thenReturn("keycloak-user-123");
        when(userProfileRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.provision(request(AdminUserProvisioningRequest.ProvisionedRole.ADMIN)))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(keycloakAdminClient).deleteUser("keycloak-user-123");
    }

    @Test
    void reportsCompensationFailure() {
        when(userProfileRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(keycloakAdminClient.createUser(any())).thenReturn("keycloak-user-123");
        doThrow(new IllegalStateException("role assignment failed"))
                .when(keycloakAdminClient).assignClientRole("keycloak-user-123", "platform-api", "STAFF");
        doThrow(new IllegalStateException("delete failed"))
                .when(keycloakAdminClient).deleteUser("keycloak-user-123");

        assertThatThrownBy(() -> service.provision(request(AdminUserProvisioningRequest.ProvisionedRole.STAFF)))
                .isInstanceOf(RegistrationCompensationException.class);
    }

    private AdminUserProvisioningRequest request(AdminUserProvisioningRequest.ProvisionedRole role) {
        return new AdminUserProvisioningRequest(
                " Test Person ", " Person@Example.com ", "+919876543210", "a-long-test-password", role);
    }
}

package io.github.abhi07799.platform.administration.application;

import io.github.abhi07799.platform.administration.api.AdminUserRoleResponse;
import io.github.abhi07799.platform.administration.api.UpdateAdminUserRoleRequest;
import io.github.abhi07799.platform.identity.infrastructure.keycloak.KeycloakAdminClient;
import io.github.abhi07799.platform.identity.infrastructure.keycloak.KeycloakLastAdminException;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserRoleServiceTest {

    private static final UUID USER_ID = UUID.fromString("f7f5d5b5-1f75-4a39-9c51-9a5d7b3d5f11");
    private static final String TARGET_SUBJECT = "target-subject";
    private static final String ACTOR_SUBJECT = "admin-subject";

    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private KeycloakAdminClient keycloakAdminClient;
    @Mock
    private AdminAuditPersistenceService auditPersistenceService;
    @Mock
    private AdminRoleChangeLockService roleChangeLockService;

    private AdminUserRoleService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserRoleService(
                userProfileRepository, keycloakAdminClient, auditPersistenceService, roleChangeLockService);
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get())
                .when(roleChangeLockService).withRoleChangeLock(any());
    }

    @Test
    void updatesKeycloakRoleAndAuditsTheChange() {
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(profile()));
        when(keycloakAdminClient.updateClientRole(TARGET_SUBJECT, "platform-api", "STAFF"))
                .thenReturn("USER");

        AdminUserRoleResponse response = service.updateRole(
                USER_ID, UpdateAdminUserRoleRequest.Role.STAFF, ACTOR_SUBJECT);

        assertThat(response.previousRole()).isEqualTo("USER");
        assertThat(response.newRole()).isEqualTo("STAFF");
        verify(auditPersistenceService).recordRoleChange(USER_ID, ACTOR_SUBJECT, "USER", "STAFF");
    }

    @Test
    void preventsAdministratorFromChangingTheirOwnRole() {
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(
                new UserProfile(ACTOR_SUBJECT, "Admin", "admin@example.com", "+919876543210")));

        assertThatThrownBy(() -> service.updateRole(
                USER_ID, UpdateAdminUserRoleRequest.Role.STAFF, ACTOR_SUBJECT))
                .isInstanceOf(AdminSelfRoleChangeException.class);

        verify(keycloakAdminClient, never()).updateClientRole(any(), any(), any());
        verify(auditPersistenceService, never()).recordRoleChange(any(), any(), any(), any());
    }

    @Test
    void preventsDemotionOfTheLastAdministrator() {
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(profile()));
        when(keycloakAdminClient.updateClientRole(TARGET_SUBJECT, "platform-api", "STAFF"))
                .thenThrow(new KeycloakLastAdminException());

        assertThatThrownBy(() -> service.updateRole(
                USER_ID, UpdateAdminUserRoleRequest.Role.STAFF, ACTOR_SUBJECT))
                .isInstanceOf(LastAdminProtectionException.class);

        verify(auditPersistenceService, never()).recordRoleChange(any(), any(), any(), any());
    }

    @Test
    void restoresPreviousKeycloakRoleWhenAuditPersistenceFails() {
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(profile()));
        when(keycloakAdminClient.updateClientRole(TARGET_SUBJECT, "platform-api", "STAFF"))
                .thenReturn("USER");
        org.mockito.Mockito.doThrow(new IllegalStateException("audit unavailable"))
                .when(auditPersistenceService).recordRoleChange(USER_ID, ACTOR_SUBJECT, "USER", "STAFF");
        when(keycloakAdminClient.restoreClientRole(TARGET_SUBJECT, "platform-api", "USER"))
                .thenReturn("STAFF");

        assertThatThrownBy(() -> service.updateRole(
                USER_ID, UpdateAdminUserRoleRequest.Role.STAFF, ACTOR_SUBJECT))
                .isInstanceOf(AdminUserRoleUpdateException.class);

        verify(keycloakAdminClient).restoreClientRole(TARGET_SUBJECT, "platform-api", "USER");
    }

    private UserProfile profile() {
        return new UserProfile(TARGET_SUBJECT, "Test Person", "person@example.com", "+919876543210");
    }
}

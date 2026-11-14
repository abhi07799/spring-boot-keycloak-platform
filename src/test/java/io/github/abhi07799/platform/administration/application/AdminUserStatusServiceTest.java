package io.github.abhi07799.platform.administration.application;

import io.github.abhi07799.platform.identity.infrastructure.keycloak.KeycloakAdminClient;
import io.github.abhi07799.platform.user.api.UserProfileResponse;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserStatusServiceTest {

    private static final UUID USER_ID = UUID.fromString("f7f5d5b5-1f75-4a39-9c51-9a5d7b3d5f11");
    private static final String KEYCLOAK_SUBJECT = "keycloak-user-123";

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @Mock
    private AdminAuditPersistenceService auditPersistenceService;

    @Mock
    private AdminRoleChangeLockService roleChangeLockService;

    private AdminUserStatusService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserStatusService(
                userProfileRepository, keycloakAdminClient, auditPersistenceService, roleChangeLockService);
        org.mockito.Mockito.doAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(0)).get())
                .when(roleChangeLockService).withRoleChangeLock(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void disablesKeycloakAccountAndPersistsDisabledProfile() {
        UserProfile profile = new UserProfile(
                KEYCLOAK_SUBJECT, "Test Person", "person@example.com", "+919876543210");
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(profile));
        when(auditPersistenceService.persistStatusChange(USER_ID, "admin-subject", true, false)).thenAnswer(invocation -> { profile.updateEnabled(false); return profile; });
        when(keycloakAdminClient.updateUserEnabled(KEYCLOAK_SUBJECT, false)).thenReturn(true);

        UserProfileResponse response = service.updateEnabledStatus(USER_ID, false, "admin-subject");

        assertThat(profile.isEnabled()).isFalse();
        assertThat(response.enabled()).isFalse();
        InOrder order = inOrder(keycloakAdminClient, auditPersistenceService);
        order.verify(keycloakAdminClient).updateUserEnabled(KEYCLOAK_SUBJECT, false);
        order.verify(auditPersistenceService).persistStatusChange(USER_ID, "admin-subject", true, false);
    }

    @Test
    void reEnablesPreviouslyDisabledAccount() {
        UserProfile profile = new UserProfile(
                KEYCLOAK_SUBJECT, "Test Person", "person@example.com", "+919876543210");
        profile.updateEnabled(false);
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(profile));
        when(auditPersistenceService.persistStatusChange(USER_ID, "admin-subject", false, true)).thenAnswer(invocation -> { profile.updateEnabled(true); return profile; });
        when(keycloakAdminClient.updateUserEnabled(KEYCLOAK_SUBJECT, true)).thenReturn(false);

        UserProfileResponse response = service.updateEnabledStatus(USER_ID, true, "admin-subject");

        assertThat(response.enabled()).isTrue();
        verify(keycloakAdminClient).updateUserEnabled(KEYCLOAK_SUBJECT, true);
    }

    @Test
    void doesNotCallKeycloakWhenApplicationUserDoesNotExist() {
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateEnabledStatus(USER_ID, false, "admin-subject"))
                .isInstanceOf(AdminUserNotFoundException.class);
        verify(keycloakAdminClient, never()).updateUserEnabled(KEYCLOAK_SUBJECT, false);
    }

    @Test
    void leavesDatabaseUntouchedWhenKeycloakUpdateFails() {
        UserProfile profile = new UserProfile(
                KEYCLOAK_SUBJECT, "Test Person", "person@example.com", "+919876543210");
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(profile));
        doThrow(new IllegalStateException("Keycloak unavailable"))
                .when(keycloakAdminClient).updateUserEnabled(KEYCLOAK_SUBJECT, false);

        assertThatThrownBy(() -> service.updateEnabledStatus(USER_ID, false, "admin-subject"))
                .isInstanceOf(KeycloakAccountStatusUpdateException.class);
        assertThat(profile.isEnabled()).isTrue();
        verify(auditPersistenceService, never()).persistStatusChange(USER_ID, "admin-subject", true, false);
    }

    @Test
    void restoresKeycloakStatusWhenDatabasePersistenceFails() {
        UserProfile profile = new UserProfile(
                KEYCLOAK_SUBJECT, "Test Person", "person@example.com", "+919876543210");
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(profile));
        when(keycloakAdminClient.updateUserEnabled(KEYCLOAK_SUBJECT, false)).thenReturn(true);
        when(auditPersistenceService.persistStatusChange(USER_ID, "admin-subject", true, false))
                .thenThrow(new DataIntegrityViolationException("database unavailable"));

        assertThatThrownBy(() -> service.updateEnabledStatus(USER_ID, false, "admin-subject"))
                .isInstanceOf(AdminUserStatusUpdateException.class);
        InOrder order = inOrder(keycloakAdminClient);
        order.verify(keycloakAdminClient).updateUserEnabled(KEYCLOAK_SUBJECT, false);
        order.verify(keycloakAdminClient).updateUserEnabled(KEYCLOAK_SUBJECT, true);
    }

    @Test
    void reportsWhenKeycloakCompensationAlsoFails() {
        UserProfile profile = new UserProfile(
                KEYCLOAK_SUBJECT, "Test Person", "person@example.com", "+919876543210");
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(profile));
        when(keycloakAdminClient.updateUserEnabled(KEYCLOAK_SUBJECT, false)).thenReturn(true);
        when(auditPersistenceService.persistStatusChange(USER_ID, "admin-subject", true, false))
                .thenThrow(new DataIntegrityViolationException("database unavailable"));
        doThrow(new IllegalStateException("Keycloak unavailable"))
                .when(keycloakAdminClient).updateUserEnabled(KEYCLOAK_SUBJECT, true);

        assertThatThrownBy(() -> service.updateEnabledStatus(USER_ID, false, "admin-subject"))
                .isInstanceOf(AdminUserStatusCompensationException.class);
    }
}

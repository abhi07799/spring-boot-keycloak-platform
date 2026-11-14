package io.github.abhi07799.platform.administration.application;

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
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserSessionServiceTest {

    private static final UUID USER_ID = UUID.fromString("f7f5d5b5-1f75-4a39-9c51-9a5d7b3d5f11");

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @Mock
    private AdminAuditPersistenceService auditPersistenceService;

    @Mock
    private AdminRoleChangeLockService roleChangeLockService;

    private AdminUserSessionService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserSessionService(
                userProfileRepository, keycloakAdminClient, auditPersistenceService, roleChangeLockService);
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get())
                .when(roleChangeLockService).withRoleChangeLock(any(), any());
    }

    @Test
    void logsOutCurrentUserUsingValidatedSubjectAndPersistsRevocation() {
        UserProfile profile = profile();
        when(userProfileRepository.findByKeycloakSubjectId("current-subject"))
                .thenReturn(Optional.of(profile));

        service.logoutCurrentUser("current-subject");

        verify(keycloakAdminClient).logoutUser("keycloak-user-123");
        verify(auditPersistenceService).recordSessionRevocation(USER_ID, "current-subject");
    }

    @Test
    void allowsAdministratorToTerminateTargetUsersSessions() {
        UserProfile profile = profile();
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(profile));

        service.logoutUser(USER_ID, "admin-subject");

        verify(keycloakAdminClient).logoutUser("keycloak-user-123");
        verify(auditPersistenceService).recordSessionRevocation(USER_ID, "admin-subject");
    }

    @Test
    void doesNotWriteAuditWhenKeycloakLogoutFails() {
        UserProfile profile = profile();
        when(userProfileRepository.findByKeycloakSubjectId("current-subject"))
                .thenReturn(Optional.of(profile));
        org.mockito.Mockito.doThrow(new IllegalStateException("Keycloak unavailable"))
                .when(keycloakAdminClient).logoutUser("keycloak-user-123");

        assertThatThrownBy(() -> service.logoutCurrentUser("current-subject"))
                .isInstanceOf(KeycloakSessionRevocationException.class);

        verify(auditPersistenceService, never()).recordSessionRevocation(any(), any());
    }

    @Test
    void reportsWhenKeycloakLogoutSucceededButLocalRevocationCouldNotBePersisted() {
        UserProfile profile = profile();
        when(userProfileRepository.findByKeycloakSubjectId("current-subject"))
                .thenReturn(Optional.of(profile));
        org.mockito.Mockito.doThrow(new IllegalStateException("database unavailable"))
                .when(auditPersistenceService).recordSessionRevocation(USER_ID, "current-subject");

        assertThatThrownBy(() -> service.logoutCurrentUser("current-subject"))
                .isInstanceOf(SessionRevocationPersistenceException.class);

        verify(keycloakAdminClient).logoutUser("keycloak-user-123");
    }

    private UserProfile profile() {
        UserProfile profile = org.mockito.Mockito.mock(UserProfile.class);
        when(profile.getId()).thenReturn(USER_ID);
        when(profile.getKeycloakSubjectId()).thenReturn("keycloak-user-123");
        return profile;
    }
}

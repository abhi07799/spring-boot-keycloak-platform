package io.github.abhi07799.platform.administration.application;

import io.github.abhi07799.platform.administration.infrastructure.persistence.AdminAuditEvent;
import io.github.abhi07799.platform.administration.infrastructure.persistence.AdminAuditEventRepository;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditPersistenceServiceTest {

    private static final UUID USER_ID = UUID.fromString("f7f5d5b5-1f75-4a39-9c51-9a5d7b3d5f11");

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private AdminAuditEventRepository auditEventRepository;

    private AdminAuditPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new AdminAuditPersistenceService(userProfileRepository, auditEventRepository);
    }

    @Test
    void persistsProvisioningAndAuditEventTogether() {
        UserProfile profile = new UserProfile("target-subject", "Test Person",
                "person@example.com", "+919876543210");
        when(userProfileRepository.saveAndFlush(any())).thenReturn(profile);

        service.persistProvisionedUser(profile, "admin-subject", "STAFF");

        ArgumentCaptor<AdminAuditEvent> event = ArgumentCaptor.forClass(AdminAuditEvent.class);
        verify(auditEventRepository).saveAndFlush(event.capture());
        assertThat(event.getValue().getActorSubjectId()).isEqualTo("admin-subject");
        assertThat(event.getValue().getAction()).isEqualTo("USER_PROVISIONED");
        assertThat(event.getValue().getAssignedRole()).isEqualTo("STAFF");
    }

    @Test
    void recordsPreviousAndNewStatusForAccountChanges() {
        UserProfile profile = new UserProfile("target-subject", "Test Person",
                "person@example.com", "+919876543210");
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(profile));
        when(userProfileRepository.saveAndFlush(profile)).thenReturn(profile);

        service.persistStatusChange(USER_ID, "admin-subject", true, false);

        assertThat(profile.isEnabled()).isFalse();
        ArgumentCaptor<AdminAuditEvent> event = ArgumentCaptor.forClass(AdminAuditEvent.class);
        verify(auditEventRepository).saveAndFlush(event.capture());
        assertThat(event.getValue().getAction()).isEqualTo("USER_DISABLED");
        assertThat(event.getValue().getPreviousEnabled()).isTrue();
        assertThat(event.getValue().getNewEnabled()).isFalse();
    }


    @Test
    void recordsSessionRevocationAndAdvancesAuthorizationTimestamp() {
        UserProfile profile = new UserProfile("target-subject", "Test Person",
                "person@example.com", "+919876543210");
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(profile));
        when(userProfileRepository.saveAndFlush(profile)).thenReturn(profile);

        service.recordSessionRevocation(USER_ID, "actor-subject");

        assertThat(profile.getAuthorizationUpdatedAt()).isAfter(java.time.Instant.EPOCH);
        ArgumentCaptor<AdminAuditEvent> event = ArgumentCaptor.forClass(AdminAuditEvent.class);
        verify(auditEventRepository).saveAndFlush(event.capture());
        assertThat(event.getValue().getAction()).isEqualTo("USER_SESSIONS_REVOKED");
        assertThat(event.getValue().getActorSubjectId()).isEqualTo("actor-subject");
        assertThat(event.getValue().getTargetUserId()).isEqualTo(USER_ID);
    }

    @Test
    void recordsPreviousAndNewApplicationRoles() {
        service.recordRoleChange(USER_ID, "admin-subject", "USER", "STAFF");

        ArgumentCaptor<AdminAuditEvent> event = ArgumentCaptor.forClass(AdminAuditEvent.class);
        verify(auditEventRepository).saveAndFlush(event.capture());
        assertThat(event.getValue().getAction()).isEqualTo("USER_ROLE_CHANGED");
        assertThat(event.getValue().getTargetUserId()).isEqualTo(USER_ID);
        assertThat(event.getValue().getActorSubjectId()).isEqualTo("admin-subject");
        assertThat(event.getValue().getPreviousRole()).isEqualTo("USER");
        assertThat(event.getValue().getNewRole()).isEqualTo("STAFF");
    }

    @Test
    void doesNotCreateNoiseForAnUnchangedStatus() {
        UserProfile profile = new UserProfile("target-subject", "Test Person",
                "person@example.com", "+919876543210");
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(profile));
        when(userProfileRepository.saveAndFlush(profile)).thenReturn(profile);

        service.persistStatusChange(USER_ID, "admin-subject", true, true);

        org.mockito.Mockito.verifyNoInteractions(auditEventRepository);
    }
}

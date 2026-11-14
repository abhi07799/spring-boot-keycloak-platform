package io.github.abhi07799.platform.administration.application;

import io.github.abhi07799.platform.identity.infrastructure.keycloak.KeycloakAdminClient;
import io.github.abhi07799.platform.identity.infrastructure.keycloak.KeycloakLastAdminException;
import io.github.abhi07799.platform.user.api.UserProfileResponse;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdminUserStatusService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserStatusService.class);

    private final UserProfileRepository userProfileRepository;
    private final KeycloakAdminClient keycloakAdminClient;
    private final AdminAuditPersistenceService auditPersistenceService;
    private final AdminRoleChangeLockService roleChangeLockService;

    public AdminUserStatusService(
            UserProfileRepository userProfileRepository,
            KeycloakAdminClient keycloakAdminClient,
            AdminAuditPersistenceService auditPersistenceService,
            AdminRoleChangeLockService roleChangeLockService) {
        this.userProfileRepository = userProfileRepository;
        this.keycloakAdminClient = keycloakAdminClient;
        this.auditPersistenceService = auditPersistenceService;
        this.roleChangeLockService = roleChangeLockService;
    }

    public UserProfileResponse updateEnabledStatus(UUID userId, boolean enabled, String actorSubjectId) {
        StatusChangeContext context = new StatusChangeContext();
        return roleChangeLockService.withRoleChangeLock(
                () -> updateStatusWhileLocked(userId, enabled, actorSubjectId, context),
                failure -> compensateStatusChange(userId, enabled, context, failure));
    }

    private UserProfileResponse updateStatusWhileLocked(
            UUID userId, boolean enabled, String actorSubjectId, StatusChangeContext context) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(AdminUserNotFoundException::new);
        context.targetKeycloakSubjectId = profile.getKeycloakSubjectId();

        boolean previousKeycloakEnabled;
        try {
            previousKeycloakEnabled = keycloakAdminClient.updateUserEnabled(
                    profile.getKeycloakSubjectId(), enabled);
        } catch (KeycloakLastAdminException exception) {
            throw new LastAdminProtectionException();
        } catch (RuntimeException exception) {
            throw new KeycloakAccountStatusUpdateException(exception);
        }

        context.previousEnabled = previousKeycloakEnabled;
        UserProfile savedProfile = auditPersistenceService.persistStatusChange(
                userId, actorSubjectId, previousKeycloakEnabled, enabled);
        return UserProfileResponse.from(savedProfile);
    }

    private void compensateStatusChange(
            UUID userId, boolean requestedEnabled, StatusChangeContext context, RuntimeException failure) {
        if (context.previousEnabled == null) {
            return;
        }
        if (context.previousEnabled != requestedEnabled) {
            try {
                keycloakAdminClient.restoreUserEnabled(context.targetKeycloakSubjectId, context.previousEnabled);
            } catch (RuntimeException compensationFailure) {
                failure.addSuppressed(compensationFailure);
                log.error("Account status compensation failed for application user id {}", userId, failure);
                throw new AdminUserStatusCompensationException(failure);
            }
        }
        throw new AdminUserStatusUpdateException(failure);
    }

    private static final class StatusChangeContext {
        private String targetKeycloakSubjectId;
        private Boolean previousEnabled;
    }
}

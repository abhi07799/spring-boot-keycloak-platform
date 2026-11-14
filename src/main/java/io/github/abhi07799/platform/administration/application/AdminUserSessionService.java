package io.github.abhi07799.platform.administration.application;

import io.github.abhi07799.platform.identity.infrastructure.keycloak.KeycloakAdminClient;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdminUserSessionService {

    private final UserProfileRepository userProfileRepository;
    private final KeycloakAdminClient keycloakAdminClient;
    private final AdminAuditPersistenceService auditPersistenceService;
    private final AdminRoleChangeLockService roleChangeLockService;

    public AdminUserSessionService(
            UserProfileRepository userProfileRepository,
            KeycloakAdminClient keycloakAdminClient,
            AdminAuditPersistenceService auditPersistenceService,
            AdminRoleChangeLockService roleChangeLockService) {
        this.userProfileRepository = userProfileRepository;
        this.keycloakAdminClient = keycloakAdminClient;
        this.auditPersistenceService = auditPersistenceService;
        this.roleChangeLockService = roleChangeLockService;
    }

    public void logoutCurrentUser(String subjectId) {
        UserProfile profile = userProfileRepository.findByKeycloakSubjectId(subjectId)
                .orElseThrow(AdminUserNotFoundException::new);
        revokeSessions(profile, subjectId);
    }

    public void logoutUser(UUID userId, String actorSubjectId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(AdminUserNotFoundException::new);
        revokeSessions(profile, actorSubjectId);
    }

    private void revokeSessions(UserProfile profile, String actorSubjectId) {
        boolean[] keycloakLogoutCompleted = {false};
        try {
            roleChangeLockService.withRoleChangeLock(() -> {
                try {
                    keycloakAdminClient.logoutUser(profile.getKeycloakSubjectId());
                } catch (RuntimeException exception) {
                    throw new KeycloakSessionRevocationException(exception);
                }
                keycloakLogoutCompleted[0] = true;
                auditPersistenceService.recordSessionRevocation(profile.getId(), actorSubjectId);
                return null;
            }, failure -> { });
        } catch (KeycloakSessionRevocationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (keycloakLogoutCompleted[0]) {
                throw new SessionRevocationPersistenceException(exception);
            }
            throw new SessionRevocationUnavailableException(exception);
        }
    }
}

package io.github.abhi07799.platform.administration.application;

import io.github.abhi07799.platform.administration.api.AdminUserRoleResponse;
import io.github.abhi07799.platform.administration.api.UpdateAdminUserRoleRequest;
import io.github.abhi07799.platform.identity.infrastructure.keycloak.KeycloakAdminClient;
import io.github.abhi07799.platform.identity.infrastructure.keycloak.KeycloakLastAdminException;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdminUserRoleService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserRoleService.class);

    private final UserProfileRepository userProfileRepository;
    private final KeycloakAdminClient keycloakAdminClient;
    private final AdminAuditPersistenceService auditPersistenceService;
    private final AdminRoleChangeLockService roleChangeLockService;

    public AdminUserRoleService(
            UserProfileRepository userProfileRepository,
            KeycloakAdminClient keycloakAdminClient,
            AdminAuditPersistenceService auditPersistenceService,
            AdminRoleChangeLockService roleChangeLockService) {
        this.userProfileRepository = userProfileRepository;
        this.keycloakAdminClient = keycloakAdminClient;
        this.auditPersistenceService = auditPersistenceService;
        this.roleChangeLockService = roleChangeLockService;
    }

    public AdminUserRoleResponse updateRole(
            UUID userId, UpdateAdminUserRoleRequest.Role requestedRole, String actorSubjectId) {
        return roleChangeLockService.withRoleChangeLock(
                () -> updateRoleWhileLocked(userId, requestedRole, actorSubjectId));
    }

    private AdminUserRoleResponse updateRoleWhileLocked(
            UUID userId, UpdateAdminUserRoleRequest.Role requestedRole, String actorSubjectId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(AdminUserNotFoundException::new);

        if (profile.getKeycloakSubjectId().equals(actorSubjectId)) {
            throw new AdminSelfRoleChangeException();
        }

        String previousRole;
        try {
            previousRole = keycloakAdminClient.updateClientRole(
                    profile.getKeycloakSubjectId(), "platform-api", requestedRole.name());
        } catch (KeycloakLastAdminException exception) {
            throw new LastAdminProtectionException();
        } catch (RuntimeException exception) {
            throw new KeycloakRoleUpdateException(exception);
        }

        if (previousRole.equals(requestedRole.name())) {
            return new AdminUserRoleResponse(userId, previousRole, requestedRole.name());
        }

        try {
            auditPersistenceService.recordRoleChange(
                    userId, actorSubjectId, previousRole, requestedRole.name());
            return new AdminUserRoleResponse(userId, previousRole, requestedRole.name());
        } catch (RuntimeException persistenceFailure) {
            try {
                keycloakAdminClient.updateClientRole(
                        profile.getKeycloakSubjectId(), "platform-api", previousRole);
            } catch (RuntimeException compensationFailure) {
                log.error("Role update compensation failed for application user id {}", userId,
                        compensationFailure);
                throw new AdminUserRoleCompensationException(compensationFailure);
            }
            throw new AdminUserRoleUpdateException(persistenceFailure);
        }
    }
}

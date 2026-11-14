package io.github.abhi07799.platform.administration.application;

import io.github.abhi07799.platform.administration.api.AdminUserProvisioningRequest;
import io.github.abhi07799.platform.identity.api.RegistrationResponse;
import io.github.abhi07799.platform.identity.application.EmailAlreadyRegisteredException;
import io.github.abhi07799.platform.identity.application.RegistrationCompensationException;
import io.github.abhi07799.platform.identity.application.RegistrationFailedException;
import io.github.abhi07799.platform.identity.infrastructure.keycloak.KeycloakAdminClient;
import io.github.abhi07799.platform.identity.infrastructure.keycloak.KeycloakUserRepresentation;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Locale;

@Service
public class AdminUserProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserProvisioningService.class);
    private static final String API_CLIENT_ID = "platform-api";

    private final KeycloakAdminClient keycloakAdminClient;
    private final UserProfileRepository userProfileRepository;
    private final AdminAuditPersistenceService auditPersistenceService;
    private final AdminRoleChangeLockService roleChangeLockService;

    public AdminUserProvisioningService(
            KeycloakAdminClient keycloakAdminClient,
            UserProfileRepository userProfileRepository,
            AdminAuditPersistenceService auditPersistenceService,
            AdminRoleChangeLockService roleChangeLockService) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.userProfileRepository = userProfileRepository;
        this.auditPersistenceService = auditPersistenceService;
        this.roleChangeLockService = roleChangeLockService;
    }

    public RegistrationResponse provision(AdminUserProvisioningRequest request, String actorSubjectId) {
        if (request.role() == AdminUserProvisioningRequest.ProvisionedRole.ADMIN) {
            return provisionAdminWhileLocked(request, actorSubjectId);
        }
        return provisionUser(request, actorSubjectId, new ProvisioningContext());
    }

    private RegistrationResponse provisionAdminWhileLocked(
            AdminUserProvisioningRequest request, String actorSubjectId) {
        ProvisioningContext context = new ProvisioningContext();
        try {
            return roleChangeLockService.withRoleChangeLock(
                    () -> provisionUser(request, actorSubjectId, context));
        } catch (DataIntegrityViolationException exception) {
            if (context.keycloakUserId == null) {
                throw exception;
            }
            compensateKeycloakUser(context.keycloakUserId, exception);
            throw new EmailAlreadyRegisteredException();
        } catch (RuntimeException exception) {
            if (context.keycloakUserId == null) {
                throw exception;
            }
            compensateKeycloakUser(context.keycloakUserId, exception);
            throw new RegistrationFailedException(
                    "User provisioning could not be completed. Please try again later.", exception);
        }
    }

    private RegistrationResponse provisionUser(
            AdminUserProvisioningRequest request, String actorSubjectId, ProvisioningContext context) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        String fullName = request.fullName().trim();
        String mobileNumber = request.mobileNumber().trim();
        String role = request.role().name();

        if (userProfileRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        KeycloakUserRepresentation keycloakUser = new KeycloakUserRepresentation(
                normalizedEmail, normalizedEmail, fullName, null, true, false, List.of(),
                List.of(new KeycloakUserRepresentation.CredentialRepresentation(
                        "password", request.password(), false)));

        try {
            context.keycloakUserId = keycloakAdminClient.createUser(keycloakUser);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 409) {
                throw new EmailAlreadyRegisteredException();
            }
            throw new RegistrationFailedException(
                    "The identity provider could not create the account. Please try again later.", exception);
        } catch (RuntimeException exception) {
            throw new RegistrationFailedException(
                    "The identity provider could not create the account. Please try again later.", exception);
        }

        if (request.role() != AdminUserProvisioningRequest.ProvisionedRole.ADMIN) {
            try {
                return assignRoleAndPersist(context.keycloakUserId, fullName, normalizedEmail,
                        mobileNumber, role, actorSubjectId);
            } catch (DataIntegrityViolationException exception) {
                compensateKeycloakUser(context.keycloakUserId, exception);
                throw new EmailAlreadyRegisteredException();
            } catch (RuntimeException exception) {
                compensateKeycloakUser(context.keycloakUserId, exception);
                throw new RegistrationFailedException(
                        "User provisioning could not be completed. Please try again later.", exception);
            }
        }

        return assignRoleAndPersist(context.keycloakUserId, fullName, normalizedEmail,
                mobileNumber, role, actorSubjectId);
    }

    private RegistrationResponse assignRoleAndPersist(
            String keycloakUserId,
            String fullName,
            String normalizedEmail,
            String mobileNumber,
            String role,
            String actorSubjectId) {
        keycloakAdminClient.assignClientRole(keycloakUserId, API_CLIENT_ID, role);
        UserProfile profile = auditPersistenceService.persistProvisionedUser(
                new UserProfile(keycloakUserId, fullName, normalizedEmail, mobileNumber),
                actorSubjectId, role);
        return new RegistrationResponse(
                profile.getId(), profile.getFullName(), profile.getEmail(), profile.getCreatedAt());
    }

    private void compensateKeycloakUser(String keycloakUserId, RuntimeException provisioningFailure) {
        try {
            keycloakAdminClient.deleteUser(keycloakUserId);
        } catch (RuntimeException compensationFailure) {
            provisioningFailure.addSuppressed(compensationFailure);
            log.error("Admin provisioning compensation failed for Keycloak user id {}",
                    keycloakUserId, provisioningFailure);
            throw new RegistrationCompensationException(provisioningFailure, compensationFailure);
        }
    }

    private static final class ProvisioningContext {
        private String keycloakUserId;
    }
}

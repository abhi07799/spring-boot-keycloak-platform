package io.github.abhi07799.platform.administration.application;

import io.github.abhi07799.platform.identity.infrastructure.keycloak.KeycloakAdminClient;
import io.github.abhi07799.platform.user.api.UserProfileResponse;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminUserStatusService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserStatusService.class);

    private final UserProfileRepository userProfileRepository;
    private final KeycloakAdminClient keycloakAdminClient;

    public AdminUserStatusService(
            UserProfileRepository userProfileRepository,
            KeycloakAdminClient keycloakAdminClient) {
        this.userProfileRepository = userProfileRepository;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    @Transactional
    public UserProfileResponse updateEnabledStatus(UUID userId, boolean enabled) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(AdminUserNotFoundException::new);
        boolean previousEnabled = profile.isEnabled();

        try {
            keycloakAdminClient.updateUserEnabled(profile.getKeycloakSubjectId(), enabled);
        } catch (RuntimeException exception) {
            throw new AdminUserStatusUpdateException(exception);
        }

        try {
            profile.updateEnabled(enabled);
            UserProfile savedProfile = userProfileRepository.saveAndFlush(profile);
            return UserProfileResponse.from(savedProfile);
        } catch (RuntimeException persistenceFailure) {
            try {
                keycloakAdminClient.updateUserEnabled(profile.getKeycloakSubjectId(), previousEnabled);
            } catch (RuntimeException compensationFailure) {
                log.error("Account status compensation failed for application user id {}", userId,
                        compensationFailure);
                throw new AdminUserStatusCompensationException(compensationFailure);
            }
            throw new AdminUserStatusUpdateException(persistenceFailure);
        }
    }
}

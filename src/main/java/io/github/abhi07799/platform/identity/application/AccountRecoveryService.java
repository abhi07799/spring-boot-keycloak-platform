package io.github.abhi07799.platform.identity.application;

import io.github.abhi07799.platform.identity.infrastructure.keycloak.KeycloakAdminClient;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class AccountRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(AccountRecoveryService.class);

    private final UserProfileRepository userProfileRepository;
    private final KeycloakAdminClient keycloakAdminClient;

    public AccountRecoveryService(
            UserProfileRepository userProfileRepository,
            KeycloakAdminClient keycloakAdminClient) {
        this.userProfileRepository = userProfileRepository;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    /**
     * Always returns normally so callers cannot distinguish an unknown email,
     * a disabled account, or a successful password-reset email request.
     */
    public void requestPasswordReset(String email) {
        Optional<UserProfile> profile = findEnabledProfile(email);
        if (profile.isEmpty()) {
            return;
        }

        try {
            keycloakAdminClient.sendPasswordResetEmail(profile.get().getKeycloakSubjectId());
        } catch (RuntimeException exception) {
            log.warn("Could not initiate password reset for application profile {}",
                    profile.get().getId(), exception);
        }
    }

    /**
     * Resends verification only for an enabled account whose email is not verified.
     * The response is deliberately identical for unknown and already verified addresses.
     */
    public void resendEmailVerification(String email) {
        Optional<UserProfile> profile = findEnabledProfile(email);
        if (profile.isEmpty()) {
            return;
        }

        UserProfile account = profile.get();
        try {
            if (!keycloakAdminClient.isEmailVerified(account.getKeycloakSubjectId())) {
                keycloakAdminClient.sendVerifyEmail(account.getKeycloakSubjectId());
            }
        } catch (RuntimeException exception) {
            log.warn("Could not initiate email verification for application profile {}",
                    account.getId(), exception);
        }
    }

    private Optional<UserProfile> findEnabledProfile(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userProfileRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .filter(UserProfile::isEnabled);
    }
}

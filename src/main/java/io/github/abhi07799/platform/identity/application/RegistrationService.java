package io.github.abhi07799.platform.identity.application;

import io.github.abhi07799.platform.identity.api.RegistrationRequest;
import io.github.abhi07799.platform.identity.api.RegistrationResponse;
import io.github.abhi07799.platform.identity.infrastructure.keycloak.KeycloakAdminClient;
import io.github.abhi07799.platform.identity.infrastructure.keycloak.KeycloakUserRepresentation;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);
    private static final String API_CLIENT_ID = "platform-api";
    private static final String DEFAULT_ROLE = "USER";

    private final KeycloakAdminClient keycloakAdminClient;
    private final UserProfileRepository userProfileRepository;

    public RegistrationService(
            KeycloakAdminClient keycloakAdminClient,
            UserProfileRepository userProfileRepository) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.userProfileRepository = userProfileRepository;
    }

    public RegistrationResponse register(RegistrationRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        String fullName = request.fullName().trim();
        String mobileNumber = request.mobileNumber().trim();

        if (userProfileRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        KeycloakUserRepresentation keycloakUser = new KeycloakUserRepresentation(
                normalizedEmail,
                normalizedEmail,
                fullName,
                null,
                true,
                false,
                List.of(),
                List.of(new KeycloakUserRepresentation.CredentialRepresentation(
                        "password", request.password(), false)));

        String keycloakUserId = keycloakAdminClient.createUser(keycloakUser);

        try {
            keycloakAdminClient.assignClientRole(keycloakUserId, API_CLIENT_ID, DEFAULT_ROLE);

            UserProfile profile = userProfileRepository.saveAndFlush(
                    new UserProfile(keycloakUserId, fullName, normalizedEmail, mobileNumber));

            return new RegistrationResponse(
                    profile.getId(),
                    profile.getFullName(),
                    profile.getEmail(),
                    profile.getCreatedAt());
        } catch (DataIntegrityViolationException exception) {
            compensateKeycloakUser(keycloakUserId, exception);
            throw new EmailAlreadyRegisteredException();
        } catch (RuntimeException exception) {
            compensateKeycloakUser(keycloakUserId, exception);
            throw new RegistrationFailedException(
                    "Registration could not be completed. Please try again later.", exception);
        }
    }

    private void compensateKeycloakUser(String keycloakUserId, RuntimeException registrationFailure) {
        try {
            keycloakAdminClient.deleteUser(keycloakUserId);
        } catch (RuntimeException compensationFailure) {
            log.error("Registration compensation failed after an unsuccessful profile registration",
                    compensationFailure);
            throw new RegistrationCompensationException(registrationFailure, compensationFailure);
        }
    }
}

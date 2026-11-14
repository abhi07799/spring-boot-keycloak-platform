package io.github.abhi07799.platform.security;

import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

public class AccountStatusJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_ACCOUNT = new OAuth2Error(
            "invalid_token",
            "The account is disabled or has no application profile.",
            null);

    private final UserProfileRepository userProfileRepository;

    public AccountStatusJwtValidator(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            return OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT);
        }

        Optional<UserProfile> profile = userProfileRepository.findByKeycloakSubjectId(subject);
        if (profile.isEmpty() || !profile.get().isEnabled()) {
            return OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT);
        }

        return OAuth2TokenValidatorResult.success();
    }
}

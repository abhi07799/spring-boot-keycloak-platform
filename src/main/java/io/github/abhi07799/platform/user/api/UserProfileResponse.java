package io.github.abhi07799.platform.user.api;

import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String fullName,
        String email,
        String mobileNumber,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserProfileResponse from(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getFullName(),
                profile.getEmail(),
                profile.getMobileNumber(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}

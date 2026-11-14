package io.github.abhi07799.platform.administration.api;

import io.github.abhi07799.platform.user.api.UserProfileResponse;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminUserDirectoryResponse(
        List<UserProfileResponse> users,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static AdminUserDirectoryResponse from(Page<UserProfile> profiles) {
        return new AdminUserDirectoryResponse(
                profiles.getContent().stream().map(UserProfileResponse::from).toList(),
                profiles.getNumber(),
                profiles.getSize(),
                profiles.getTotalElements(),
                profiles.getTotalPages(),
                profiles.isFirst(),
                profiles.isLast()
        );
    }
}

package io.github.abhi07799.platform.administration.api;

import java.util.UUID;

public record AdminUserRoleResponse(UUID userId, String previousRole, String newRole) {
}

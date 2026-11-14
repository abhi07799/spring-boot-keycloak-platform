package io.github.abhi07799.platform.administration.api;

import jakarta.validation.constraints.NotNull;

public record UpdateAdminUserRoleRequest(@NotNull Role role) {

    public enum Role {
        USER,
        STAFF,
        ADMIN
    }
}

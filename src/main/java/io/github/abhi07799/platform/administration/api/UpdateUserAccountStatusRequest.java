package io.github.abhi07799.platform.administration.api;

import jakarta.validation.constraints.NotNull;

public record UpdateUserAccountStatusRequest(@NotNull Boolean enabled) {
}

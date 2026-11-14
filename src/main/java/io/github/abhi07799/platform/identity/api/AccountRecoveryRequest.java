package io.github.abhi07799.platform.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountRecoveryRequest(
        @NotBlank @Email @Size(max = 320) String email) {
}

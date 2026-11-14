package io.github.abhi07799.platform.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "must contain 10 to 15 digits with an optional leading +")
        String mobileNumber,
        @NotBlank @Size(min = 12, max = 128) String password) {
}

package io.github.abhi07799.platform.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9]{10,15}$",
                message = "must contain 10 to 15 digits with an optional leading +")
        String mobileNumber) {
}

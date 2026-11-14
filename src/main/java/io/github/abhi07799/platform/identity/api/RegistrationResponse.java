package io.github.abhi07799.platform.identity.api;

import java.time.Instant;
import java.util.UUID;

public record RegistrationResponse(
        UUID id,
        String fullName,
        String email,
        Instant createdAt) {
}

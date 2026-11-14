package io.github.abhi07799.platform.administration.api;

import io.github.abhi07799.platform.administration.infrastructure.persistence.AdminAuditEvent;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditEventResponse(
        UUID id,
        String actorSubjectId,
        String action,
        UUID targetUserId,
        String assignedRole,
        Boolean previousEnabled,
        Boolean newEnabled,
        String previousRole,
        String newRole,
        Instant occurredAt
) {
    public static AdminAuditEventResponse from(AdminAuditEvent event) {
        return new AdminAuditEventResponse(
                event.getId(),
                event.getActorSubjectId(),
                event.getAction(),
                event.getTargetUserId(),
                event.getAssignedRole(),
                event.getPreviousEnabled(),
                event.getNewEnabled(),
                event.getPreviousRole(),
                event.getNewRole(),
                event.getOccurredAt());
    }
}

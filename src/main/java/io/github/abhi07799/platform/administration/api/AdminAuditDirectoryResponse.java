package io.github.abhi07799.platform.administration.api;

import io.github.abhi07799.platform.administration.infrastructure.persistence.AdminAuditEvent;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminAuditDirectoryResponse(
        List<AdminAuditEventResponse> events,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static AdminAuditDirectoryResponse from(Page<AdminAuditEvent> events) {
        return new AdminAuditDirectoryResponse(
                events.getContent().stream().map(AdminAuditEventResponse::from).toList(),
                events.getNumber(),
                events.getSize(),
                events.getTotalElements(),
                events.getTotalPages(),
                events.isFirst(),
                events.isLast());
    }
}

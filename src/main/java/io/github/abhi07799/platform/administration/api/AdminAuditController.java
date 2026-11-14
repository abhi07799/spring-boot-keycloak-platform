package io.github.abhi07799.platform.administration.api;

import io.github.abhi07799.platform.administration.application.AdminAuditQueryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-events")
public class AdminAuditController {

    private final AdminAuditQueryService queryService;

    public AdminAuditController(AdminAuditQueryService queryService) {
        this.queryService = queryService;
    }

    @Operation(summary = "List administrative audit events (ADMIN only)")
    @GetMapping
    public AdminAuditDirectoryResponse listEvents(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return AdminAuditDirectoryResponse.from(queryService.listEvents(page, size));
    }
}

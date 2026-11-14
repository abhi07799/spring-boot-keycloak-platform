package io.github.abhi07799.platform.administration.api;

import io.github.abhi07799.platform.administration.application.AdminUserDirectoryService;
import io.github.abhi07799.platform.administration.application.AdminUserProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/admin/users")
public class AdminUserProvisioningController {

    private final AdminUserProvisioningService provisioningService;
    private final AdminUserDirectoryService directoryService;

    public AdminUserProvisioningController(
            AdminUserProvisioningService provisioningService,
            AdminUserDirectoryService directoryService) {
        this.provisioningService = provisioningService;
        this.directoryService = directoryService;
    }

    @Operation(summary = "List application users (ADMIN only)")
    @GetMapping
    public AdminUserDirectoryResponse listUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return AdminUserDirectoryResponse.from(directoryService.listUsers(page, size));
    }

    @Operation(summary = "Create a STAFF or ADMIN account (ADMIN only)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse createUser(@Valid @RequestBody AdminUserProvisioningRequest request) {
        return provisioningService.provision(request);
    }
}

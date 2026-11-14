package io.github.abhi07799.platform.administration.api;

import io.github.abhi07799.platform.identity.api.RegistrationResponse;
import io.github.abhi07799.platform.administration.application.AdminUserProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserProvisioningController {

    private final AdminUserProvisioningService provisioningService;

    public AdminUserProvisioningController(AdminUserProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    @Operation(summary = "Create a STAFF or ADMIN account (ADMIN only)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse createUser(@Valid @RequestBody AdminUserProvisioningRequest request) {
        return provisioningService.provision(request);
    }
}

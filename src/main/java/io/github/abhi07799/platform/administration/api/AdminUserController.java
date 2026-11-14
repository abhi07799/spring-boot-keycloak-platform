package io.github.abhi07799.platform.administration.api;

import io.github.abhi07799.platform.administration.application.AdminUserDirectoryService;
import io.github.abhi07799.platform.administration.application.AdminUserProvisioningService;
import io.github.abhi07799.platform.administration.application.AdminUserStatusService;
import io.github.abhi07799.platform.administration.application.AdminUserRoleService;
import io.github.abhi07799.platform.administration.application.AdminUserSessionService;
import io.github.abhi07799.platform.identity.api.RegistrationResponse;
import io.github.abhi07799.platform.user.api.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserProvisioningService provisioningService;
    private final AdminUserDirectoryService directoryService;
    private final AdminUserStatusService statusService;
    private final AdminUserRoleService roleService;
    private final AdminUserSessionService sessionService;

    public AdminUserController(
            AdminUserProvisioningService provisioningService,
            AdminUserDirectoryService directoryService,
            AdminUserStatusService statusService,
            AdminUserRoleService roleService,
            AdminUserSessionService sessionService) {
        this.provisioningService = provisioningService;
        this.directoryService = directoryService;
        this.statusService = statusService;
        this.roleService = roleService;
        this.sessionService = sessionService;
    }

    @Operation(summary = "List application users (ADMIN only)")
    @GetMapping
    public AdminUserDirectoryResponse listUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return AdminUserDirectoryResponse.from(directoryService.listUsers(page, size));
    }

    @Operation(summary = "Enable or disable an application account (ADMIN only)")
    @PatchMapping("/{userId}/status")
    public UserProfileResponse updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserAccountStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return statusService.updateEnabledStatus(userId, request.enabled(), jwt.getSubject());
    }

    @Operation(summary = "Update an application user role (ADMIN only)")
    @PatchMapping("/{userId}/role")
    public AdminUserRoleResponse updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateAdminUserRoleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return roleService.updateRole(userId, request.role(), jwt.getSubject());
    }

    @Operation(summary = "Terminate all sessions for an application user (ADMIN only)")
    @PostMapping("/{userId}/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutUser(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {
        sessionService.logoutUser(userId, jwt.getSubject());
    }

    @Operation(summary = "Create a STAFF or ADMIN account (ADMIN only)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse createUser(
            @Valid @RequestBody AdminUserProvisioningRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return provisioningService.provision(request, jwt.getSubject());
    }
}

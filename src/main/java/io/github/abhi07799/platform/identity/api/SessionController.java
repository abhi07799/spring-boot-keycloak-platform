package io.github.abhi07799.platform.identity.api;

import io.github.abhi07799.platform.administration.application.AdminUserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class SessionController {

    private final AdminUserSessionService sessionService;

    public SessionController(AdminUserSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Operation(summary = "Log out the current user from all Keycloak sessions")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Jwt jwt) {
        sessionService.logoutCurrentUser(jwt.getSubject());
    }
}

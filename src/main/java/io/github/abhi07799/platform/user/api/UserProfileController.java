package io.github.abhi07799.platform.user.api;

import io.github.abhi07799.platform.user.application.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @Operation(summary = "Get the authenticated user's application profile")
    @GetMapping("/me")
    public UserProfileResponse getCurrentUserProfile(@AuthenticationPrincipal Jwt jwt) {
        return UserProfileResponse.from(
                userProfileService.getByKeycloakSubjectId(jwt.getSubject())
        );
    }
}

package io.github.abhi07799.platform.user.api;

import io.github.abhi07799.platform.user.application.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
        return UserProfileResponse.from(userProfileService.getByKeycloakSubjectId(jwt.getSubject()));
    }

    @Operation(summary = "Update the authenticated user's editable profile fields")
    @PutMapping("/me")
    public UserProfileResponse updateCurrentUserProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return UserProfileResponse.from(userProfileService.updateProfile(
                jwt.getSubject(), request.fullName(), request.mobileNumber()));
    }
}

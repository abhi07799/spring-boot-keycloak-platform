package io.github.abhi07799.platform.user.api;

import io.github.abhi07799.platform.security.KeycloakRoleConverter;
import io.github.abhi07799.platform.security.config.SecurityConfig;
import io.github.abhi07799.platform.user.application.UserProfileNotFoundException;
import io.github.abhi07799.platform.user.application.UserProfileService;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@Import({SecurityConfig.class, KeycloakRoleConverter.class, UserProfileExceptionHandler.class,
        UserProfileControllerTest.TestConfigurationBeans.class})
class UserProfileControllerTest {

    private static final String SUBJECT_ID = "keycloak-subject-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserProfileService userProfileService;

    @Test
    void currentProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsOnlyProfileForAuthenticatedJwtSubject() throws Exception {
        UserProfile profile = new UserProfile(
                SUBJECT_ID, "Test Person", "person@example.com", "+919876543210");
        when(userProfileService.getByKeycloakSubjectId(SUBJECT_ID)).thenReturn(profile);

        mockMvc.perform(get("/api/v1/users/me").with(jwt().jwt(token -> token.subject(SUBJECT_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Test Person"))
                .andExpect(jsonPath("$.email").value("person@example.com"))
                .andExpect(jsonPath("$.mobileNumber").value("+919876543210"))
                .andExpect(jsonPath("$.keycloakSubjectId").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void returnsNotFoundWhenAuthenticatedSubjectHasNoApplicationProfile() throws Exception {
        when(userProfileService.getByKeycloakSubjectId(SUBJECT_ID))
                .thenThrow(new UserProfileNotFoundException());

        mockMvc.perform(get("/api/v1/users/me").with(jwt().jwt(token -> token.subject(SUBJECT_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @TestConfiguration
    static class TestConfigurationBeans {

        @Bean
        @Primary
        UserProfileService userProfileService() {
            return mock(UserProfileService.class);
        }

        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            return token -> {
                throw new JwtException("Mock JWT authentication is supplied by the test request.");
            };
        }
    }
}

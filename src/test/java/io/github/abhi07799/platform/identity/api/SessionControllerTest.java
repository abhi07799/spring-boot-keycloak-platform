package io.github.abhi07799.platform.identity.api;

import io.github.abhi07799.platform.administration.application.AdminUserSessionService;
import io.github.abhi07799.platform.administration.api.AdminUserExceptionHandler;
import io.github.abhi07799.platform.security.KeycloakRoleConverter;
import io.github.abhi07799.platform.security.config.SecurityConfig;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfileRepository;
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
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
@Import({SecurityConfig.class, KeycloakRoleConverter.class, AdminUserExceptionHandler.class,
        SessionControllerTest.TestBeans.class})
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserSessionService sessionService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logsOutTheSubjectFromTheValidatedJwt() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(jwt().jwt(token -> token.subject("current-user-subject"))))
                .andExpect(status().isNoContent());

        verify(sessionService).logoutCurrentUser("current-user-subject");
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        @Primary
        AdminUserSessionService sessionService() {
            return mock(AdminUserSessionService.class);
        }

        @Bean
        @Primary
        UserProfileRepository userProfileRepository() {
            return mock(UserProfileRepository.class);
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

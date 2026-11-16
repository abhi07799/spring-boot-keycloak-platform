package io.github.abhi07799.platform.identity.api;

import io.github.abhi07799.platform.identity.application.RegistrationService;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistrationController.class)
@TestPropertySource(properties = "security.rate-limit.hmac-secret=0123456789abcdef0123456789abcdef")
@Import({SecurityConfig.class, KeycloakRoleConverter.class, RegistrationControllerTest.TestConfigurationBeans.class})
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistrationService registrationService;

    @Test
    void registrationEndpointIsPublicAndReturnsCreated() throws Exception {
        when(registrationService.register(any())).thenReturn(new RegistrationResponse(
                UUID.fromString("a5e5c1e8-5b11-4d78-8d7d-1b10e13f42b7"),
                "Test Person", "person@example.com", Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Test Person",
                                  "email": "person@example.com",
                                  "mobileNumber": "+919876543210",
                                  "password": "a-long-test-password"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("person@example.com"))
                .andExpect(jsonPath("$.id").value("a5e5c1e8-5b11-4d78-8d7d-1b10e13f42b7"));
    }

    @Test
    void rejectsInvalidRegistrationPayload() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "",
                                  "email": "not-an-email",
                                  "mobileNumber": "123",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrationDoesNotReturnPassword() throws Exception {
        when(registrationService.register(any())).thenReturn(new RegistrationResponse(
                UUID.fromString("a5e5c1e8-5b11-4d78-8d7d-1b10e13f42b7"),
                "Test Person", "person@example.com", Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Test Person",
                                  "email": "person@example.com",
                                  "mobileNumber": "9876543210",
                                  "password": "a-long-test-password"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @TestConfiguration
    static class TestConfigurationBeans {

        @Bean
        @Primary
        RegistrationService registrationService() {
            return mock(RegistrationService.class);
        }

        @Bean
        @Primary
        UserProfileRepository userProfileRepository() {
            return mock(UserProfileRepository.class);
        }

        @Bean
        @Primary
        JdbcTemplate jdbcTemplate() {
            JdbcTemplate template = mock(JdbcTemplate.class);
            when(template.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(1);
            return template;
        }

        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            return token -> {
                throw new JwtException("No JWT is expected in this MVC slice test.");
            };
        }
    }
}

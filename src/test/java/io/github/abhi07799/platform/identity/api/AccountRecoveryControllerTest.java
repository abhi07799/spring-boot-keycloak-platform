package io.github.abhi07799.platform.identity.api;

import io.github.abhi07799.platform.identity.application.AccountRecoveryService;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountRecoveryController.class)
@Import({SecurityConfig.class, KeycloakRoleConverter.class, AccountRecoveryControllerTest.TestBeans.class})
class AccountRecoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRecoveryService accountRecoveryService;

    @Test
    void passwordResetRequestIsPublicAndReturnsGenericAcceptedResponse() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"person@example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value(
                        "If the account is eligible, an email with the requested instructions will be sent."));

        verify(accountRecoveryService).requestPasswordReset("person@example.com");
    }

    @Test
    void emailVerificationResendIsPublicAndReturnsGenericAcceptedResponse() throws Exception {
        mockMvc.perform(post("/api/v1/auth/email/verification/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"person@example.com\"}"))
                .andExpect(status().isAccepted());

        verify(accountRecoveryService).resendEmailVerification("person@example.com");
    }

    @Test
    void rejectsInvalidEmailPayload() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        @Primary
        AccountRecoveryService accountRecoveryService() {
            return mock(AccountRecoveryService.class);
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

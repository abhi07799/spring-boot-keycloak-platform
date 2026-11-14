package io.github.abhi07799.platform.system.api;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemController.class)
@Import({SecurityConfig.class, KeycloakRoleConverter.class, SystemControllerTest.TestJwtDecoderConfiguration.class})
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pingReturnsPongWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("pong"));
    }

    @Test
    void otherApiPathsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/private-check"))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class TestJwtDecoderConfiguration {

        @Bean
        @Primary
        UserProfileRepository userProfileRepository() {
            return mock(UserProfileRepository.class);
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

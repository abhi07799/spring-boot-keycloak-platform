package io.github.abhi07799.platform.administration.api;

import io.github.abhi07799.platform.administration.application.AdminAuditQueryService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuditController.class)
@Import({SecurityConfig.class, KeycloakRoleConverter.class, AdminAuditControllerTest.TestConfigurationBeans.class})
class AdminAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminAuditQueryService queryService;

    @Test
    void requiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-events"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/audit-events")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsPaginatedAuditEventsForAdmin() throws Exception {
        when(queryService.listEvents(0, 20)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/admin/audit-events")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(queryService).listEvents(0, 20);
    }

    @TestConfiguration
    static class TestConfigurationBeans {
        @Bean
        @Primary
        AdminAuditQueryService queryService() {
            return mock(AdminAuditQueryService.class);
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

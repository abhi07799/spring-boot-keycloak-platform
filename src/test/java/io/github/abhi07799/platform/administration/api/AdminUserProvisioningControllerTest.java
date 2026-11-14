package io.github.abhi07799.platform.administration.api;

import io.github.abhi07799.platform.administration.application.AdminUserDirectoryService;
import io.github.abhi07799.platform.administration.application.AdminUserProvisioningService;
import io.github.abhi07799.platform.identity.api.RegistrationExceptionHandler;
import io.github.abhi07799.platform.security.KeycloakRoleConverter;
import io.github.abhi07799.platform.security.config.SecurityConfig;
import io.github.abhi07799.platform.user.infrastructure.persistence.UserProfile;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserProvisioningController.class)
@Import({SecurityConfig.class, KeycloakRoleConverter.class, RegistrationExceptionHandler.class,
        AdminUserProvisioningControllerTest.TestConfigurationBeans.class})
class AdminUserProvisioningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserProvisioningService provisioningService;

    @Autowired
    private AdminUserDirectoryService directoryService;

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAuthenticatedNonAdminRequests() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminToListUsersWithPaginationMetadata() throws Exception {
        UserProfile profile = new UserProfile(
                "subject-1", "Test Person", "person@example.com", "+919876543210");
        when(directoryService.listUsers(0, 20))
                .thenReturn(new PageImpl<>(List.of(profile), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].fullName").value("Test Person"))
                .andExpect(jsonPath("$.users[0].email").value("person@example.com"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.users[0].keycloakSubjectId").doesNotExist())
                .andExpect(jsonPath("$.users[0].password").doesNotExist());

        verify(directoryService).listUsers(0, 20);
    }

    @Test
    void rejectsPageSizesAboveTheMaximum() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("size", "101")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allowsAdminToProvisionUserAndReturnsCreated() throws Exception {
        when(provisioningService.provision(any())).thenReturn(null);

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType("application/json")
                        .content("""
                                {
                                  "fullName": "Test Person",
                                  "email": "person@example.com",
                                  "mobileNumber": "+919876543210",
                                  "password": "a-long-test-password",
                                  "role": "STAFF"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(provisioningService).provision(any());
    }

    @Test
    void validatesRoleAndRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType("application/json")
                        .content("""
                                {
                                  "fullName": "Test Person",
                                  "email": "person@example.com",
                                  "mobileNumber": "+919876543210",
                                  "password": "a-long-test-password",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class TestConfigurationBeans {

        @Bean
        @Primary
        AdminUserProvisioningService provisioningService() {
            return mock(AdminUserProvisioningService.class);
        }

        @Bean
        @Primary
        AdminUserDirectoryService directoryService() {
            return mock(AdminUserDirectoryService.class);
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

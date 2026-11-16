package io.github.abhi07799.platform.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicAuthRateLimitFilterTest {

    private JdbcTemplate jdbcTemplate;
    private PublicAuthRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        filter = new PublicAuthRateLimitFilter(
                jdbcTemplate,
                "0123456789abcdef0123456789abcdef",
                Clock.fixed(Instant.parse("2026-11-16T12:00:30Z"), ZoneOffset.UTC));
    }

    @Test
    void allowsRequestWithinConfiguredLimit() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenReturn(1);
        MockHttpServletRequest request = request("/api/v1/auth/register");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(request, chain.getRequest());
        verify(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), any(Object[].class));
    }

    @Test
    void rejectsRequestAfterLimitAndReturnsRetryAfterHeader() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenReturn(11);
        MockHttpServletRequest request = request("/api/v1/auth/register");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(429, response.getStatus());
        assertEquals("30", response.getHeader("Retry-After"));
        assertEquals(true, response.getContentAsString().contains("Too many requests"));
    }

    @Test
    void failsClosedWhenRateLimitStorageIsUnavailable() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenThrow(new IllegalStateException("database unavailable"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/api/v1/auth/password/forgot"), response, new MockFilterChain());

        assertEquals(503, response.getStatus());
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        request.setRemoteAddr("203.0.113.10");
        return request;
    }
}

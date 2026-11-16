package io.github.abhi07799.platform.security.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

@Component
public class PublicAuthRateLimitFilter extends OncePerRequestFilter {

    private static final String UPSERT_BUCKET = """
            INSERT INTO public_auth_rate_limit_buckets
                (bucket_key, window_start_epoch, request_count, expires_at)
            VALUES (?, ?, 1, ?)
            ON CONFLICT (bucket_key, window_start_epoch)
            DO UPDATE SET request_count = public_auth_rate_limit_buckets.request_count + 1
            RETURNING request_count
            """;

    private static final Map<String, RateLimitPolicy> POLICIES = Map.of(
            "/api/v1/auth/register", new RateLimitPolicy(10, 60),
            "/api/v1/auth/password/forgot", new RateLimitPolicy(5, 900),
            "/api/v1/auth/email/verification/resend", new RateLimitPolicy(5, 900)
    );

    private final JdbcTemplate jdbcTemplate;
    private final byte[] hmacSecret;
    private final Clock clock;

    public PublicAuthRateLimitFilter(
            JdbcTemplate jdbcTemplate,
            @Value("${security.rate-limit.hmac-secret}") String hmacSecret) {
        this(jdbcTemplate, hmacSecret, Clock.systemUTC());
    }

    PublicAuthRateLimitFilter(JdbcTemplate jdbcTemplate, String hmacSecret, Clock clock) {
        if (hmacSecret == null || hmacSecret.length() < 32) {
            throw new IllegalArgumentException("Rate-limit HMAC secret must contain at least 32 characters");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.hmacSecret = hmacSecret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !POLICIES.containsKey(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        RateLimitPolicy policy = POLICIES.get(request.getServletPath());
        Instant now = clock.instant();
        long windowStart = (now.getEpochSecond() / policy.windowSeconds()) * policy.windowSeconds();
        Instant expiresAt = Instant.ofEpochSecond(windowStart + policy.windowSeconds() + 86_400);
        String bucketKey = createBucketKey(request.getServletPath(), request.getRemoteAddr());

        final int requestCount;
        try {
            Integer count = jdbcTemplate.queryForObject(
                    UPSERT_BUCKET,
                    Integer.class,
                    bucketKey,
                    windowStart,
                    expiresAt);
            requestCount = count == null ? policy.limit() + 1 : count;
        } catch (RuntimeException persistenceFailure) {
            // Fail closed: public authentication endpoints must not bypass limits when storage is unavailable.
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"status":503,"error":"Service temporarily unavailable"}
                    """.trim());
            return;
        }

        if (requestCount > policy.limit()) {
            long retryAfter = Math.max(1, windowStart + policy.windowSeconds() - now.getEpochSecond());
            response.setStatus(429);
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"status":429,"error":"Too many requests","message":"Please try again later."}
                    """.trim());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String createBucketKey(String path, String remoteAddress) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret, "HmacSHA256"));
            byte[] digest = mac.doFinal((path + ":" + remoteAddress).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create rate-limit bucket key", exception);
        }
    }

    private record RateLimitPolicy(int limit, long windowSeconds) {
    }
}

package io.github.abhi07799.platform.security.ratelimit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RateLimitBucketCleanup {

    private final JdbcTemplate jdbcTemplate;

    public RateLimitBucketCleanup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelayString = "${security.rate-limit.cleanup-interval-ms:3600000}")
    public void deleteExpiredBuckets() {
        jdbcTemplate.update(
                "DELETE FROM public_auth_rate_limit_buckets WHERE expires_at < CURRENT_TIMESTAMP");
    }
}

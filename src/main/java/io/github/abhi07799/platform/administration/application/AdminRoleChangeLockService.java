package io.github.abhi07799.platform.administration.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Service
public class AdminRoleChangeLockService {

    private final JdbcTemplate jdbcTemplate;

    public AdminRoleChangeLockService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public <T> T withRoleChangeLock(Supplier<T> operation) {
        jdbcTemplate.queryForObject(
                "SELECT lock_id FROM admin_role_change_lock WHERE lock_id = 1 FOR UPDATE",
                Integer.class);
        return operation.get();
    }
}

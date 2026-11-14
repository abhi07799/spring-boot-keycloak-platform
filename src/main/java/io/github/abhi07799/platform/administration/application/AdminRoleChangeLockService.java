package io.github.abhi07799.platform.administration.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Service
public class AdminRoleChangeLockService {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public AdminRoleChangeLockService(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public <T> T withRoleChangeLock(Supplier<T> operation) {
        return transactionTemplate.execute(status -> {
            jdbcTemplate.queryForObject(
                    "SELECT lock_id FROM admin_role_change_lock WHERE lock_id = 1 FOR UPDATE",
                    Integer.class);
            return operation.get();
        });
    }
}

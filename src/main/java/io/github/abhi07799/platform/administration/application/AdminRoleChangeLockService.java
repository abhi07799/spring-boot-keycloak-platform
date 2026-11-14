package io.github.abhi07799.platform.administration.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
public class AdminRoleChangeLockService {

    private static final Logger log = LoggerFactory.getLogger(AdminRoleChangeLockService.class);
    private static final long ADVISORY_LOCK_KEY = 0x504C4154464F524DL;

    private final DataSource dataSource;
    private final TransactionTemplate transactionTemplate;

    public AdminRoleChangeLockService(
            DataSource dataSource,
            PlatformTransactionManager transactionManager) {
        this.dataSource = dataSource;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Holds a PostgreSQL session-level advisory lock across the transaction and any
     * compensating action. The lock survives transaction rollback and serializes
     * lifecycle operations across application instances.
     */
    public <T> T withRoleChangeLock(Supplier<T> operation, Consumer<RuntimeException> onFailure) {
        try (Connection lockConnection = dataSource.getConnection()) {
            acquireLock(lockConnection);
            try {
                try {
                    return transactionTemplate.execute(status -> operation.get());
                } catch (RuntimeException failure) {
                    onFailure.accept(failure);
                    throw failure;
                }
            } finally {
                releaseLock(lockConnection);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not acquire or release the application role-change lock.", exception);
        }
    }

    private void acquireLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_lock(?)")) {
            statement.setLong(1, ADVISORY_LOCK_KEY);
            statement.execute();
        } catch (SQLException exception) {
            abortConnection(connection);
            throw exception;
        }
    }

    private void releaseLock(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            statement.setLong(1, ADVISORY_LOCK_KEY);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next() && !result.getBoolean(1)) {
                    log.error("PostgreSQL role-change advisory lock was not held by the current session.");
                }
            }
        } catch (SQLException exception) {
            log.error("Could not explicitly release the PostgreSQL role-change advisory lock; aborting the pooled connection.",
                    exception);
            abortConnection(connection);
        }
    }

    private void abortConnection(Connection connection) {
        try {
            connection.abort(Runnable::run);
        } catch (SQLException abortFailure) {
            log.error("Could not abort the PostgreSQL advisory-lock connection.", abortFailure);
        }
    }
}

package io.github.abhi07799.platform.administration.application;

public class AdminUserStatusUpdateException extends RuntimeException {

    public AdminUserStatusUpdateException(Throwable cause) {
        super("The account status could not be updated.", cause);
    }
}

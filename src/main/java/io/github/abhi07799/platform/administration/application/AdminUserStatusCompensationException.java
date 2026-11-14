package io.github.abhi07799.platform.administration.application;

public class AdminUserStatusCompensationException extends RuntimeException {

    public AdminUserStatusCompensationException(Throwable cause) {
        super("Account status synchronization and compensation both failed.", cause);
    }
}

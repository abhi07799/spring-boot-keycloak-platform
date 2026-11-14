package io.github.abhi07799.platform.administration.application;

public class AdminUserRoleCompensationException extends RuntimeException {
    public AdminUserRoleCompensationException(Throwable cause) {
        super("The account role update and its compensation both failed.", cause);
    }
}

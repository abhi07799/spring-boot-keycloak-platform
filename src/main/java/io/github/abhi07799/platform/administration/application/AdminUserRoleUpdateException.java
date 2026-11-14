package io.github.abhi07799.platform.administration.application;

public class AdminUserRoleUpdateException extends RuntimeException {
    public AdminUserRoleUpdateException(Throwable cause) {
        super("The account role could not be audited.", cause);
    }
}

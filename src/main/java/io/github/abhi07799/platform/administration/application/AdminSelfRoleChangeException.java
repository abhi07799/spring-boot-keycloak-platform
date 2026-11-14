package io.github.abhi07799.platform.administration.application;

public class AdminSelfRoleChangeException extends RuntimeException {
    public AdminSelfRoleChangeException() {
        super("Administrators cannot change their own application role.");
    }
}

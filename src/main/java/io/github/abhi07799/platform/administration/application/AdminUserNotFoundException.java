package io.github.abhi07799.platform.administration.application;

public class AdminUserNotFoundException extends RuntimeException {

    public AdminUserNotFoundException() {
        super("The requested application user was not found.");
    }
}

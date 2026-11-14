package io.github.abhi07799.platform.administration.application;

public class LastAdminProtectionException extends RuntimeException {
    public LastAdminProtectionException() {
        super("The last active application administrator cannot be demoted.");
    }
}

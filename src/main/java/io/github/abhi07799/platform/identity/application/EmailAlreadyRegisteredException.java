package io.github.abhi07799.platform.identity.application;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException() {
        super("An account with this email address already exists");
    }
}

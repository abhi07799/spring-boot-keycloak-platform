package io.github.abhi07799.platform.identity.application;

public class RegistrationFailedException extends RuntimeException {

    public RegistrationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

package io.github.abhi07799.platform.identity.application;

public class RegistrationCompensationException extends RuntimeException {

    public RegistrationCompensationException(Throwable registrationFailure, Throwable compensationFailure) {
        super("Registration failed and the temporary Keycloak account could not be removed",
                registrationFailure);
        addSuppressed(compensationFailure);
    }
}

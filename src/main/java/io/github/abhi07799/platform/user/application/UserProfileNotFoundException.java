package io.github.abhi07799.platform.user.application;

public class UserProfileNotFoundException extends RuntimeException {

    public UserProfileNotFoundException() {
        super("The authenticated user's application profile was not found.");
    }
}

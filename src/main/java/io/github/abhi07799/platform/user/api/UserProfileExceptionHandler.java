package io.github.abhi07799.platform.user.api;

import io.github.abhi07799.platform.user.application.UserProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserProfileExceptionHandler {

    @ExceptionHandler(UserProfileNotFoundException.class)
    ResponseEntity<ProblemDetail> handleProfileNotFound(UserProfileNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
}

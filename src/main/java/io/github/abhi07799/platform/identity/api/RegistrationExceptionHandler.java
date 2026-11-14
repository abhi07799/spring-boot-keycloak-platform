package io.github.abhi07799.platform.identity.api;

import io.github.abhi07799.platform.identity.application.EmailAlreadyRegisteredException;
import io.github.abhi07799.platform.identity.application.RegistrationCompensationException;
import io.github.abhi07799.platform.identity.application.RegistrationFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RegistrationExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ResponseEntity<ProblemDetail> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("The registration request is invalid");
        return problem(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler({RegistrationFailedException.class, RegistrationCompensationException.class})
    ResponseEntity<ProblemDetail> handleRegistrationFailure(RuntimeException exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Registration could not be completed. Please try again later.");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(problem);
    }
}

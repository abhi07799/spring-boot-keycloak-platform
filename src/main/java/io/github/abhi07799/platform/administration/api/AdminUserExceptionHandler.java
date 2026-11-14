package io.github.abhi07799.platform.administration.api;

import io.github.abhi07799.platform.administration.application.AdminUserNotFoundException;
import io.github.abhi07799.platform.administration.application.AdminUserStatusCompensationException;
import io.github.abhi07799.platform.administration.application.AdminUserStatusUpdateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AdminUserExceptionHandler {

    @ExceptionHandler(AdminUserNotFoundException.class)
    ResponseEntity<ProblemDetail> handleUserNotFound(AdminUserNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(AdminUserStatusUpdateException.class)
    ResponseEntity<ProblemDetail> handleStatusUpdateFailure(AdminUserStatusUpdateException exception) {
        return problem(HttpStatus.BAD_GATEWAY,
                "The account status could not be synchronized. Please try again later.");
    }

    @ExceptionHandler(AdminUserStatusCompensationException.class)
    ResponseEntity<ProblemDetail> handleCompensationFailure(AdminUserStatusCompensationException exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Account status synchronization failed and requires operational reconciliation.");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(problem);
    }
}

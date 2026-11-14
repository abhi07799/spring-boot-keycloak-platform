package io.github.abhi07799.platform.administration.api;

import io.github.abhi07799.platform.administration.application.AdminUserNotFoundException;
import io.github.abhi07799.platform.administration.application.KeycloakAccountStatusUpdateException;
import io.github.abhi07799.platform.administration.application.AdminUserStatusCompensationException;
import io.github.abhi07799.platform.administration.application.AdminUserStatusUpdateException;
import io.github.abhi07799.platform.administration.application.AdminSelfRoleChangeException;
import io.github.abhi07799.platform.administration.application.LastAdminProtectionException;
import io.github.abhi07799.platform.administration.application.KeycloakRoleUpdateException;
import io.github.abhi07799.platform.administration.application.AdminUserRoleUpdateException;
import io.github.abhi07799.platform.administration.application.AdminUserRoleCompensationException;
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

    @ExceptionHandler(KeycloakAccountStatusUpdateException.class)
    ResponseEntity<ProblemDetail> handleKeycloakStatusUpdateFailure(KeycloakAccountStatusUpdateException exception) {
        return problem(HttpStatus.BAD_GATEWAY,
                "The identity provider could not update the account status. Please try again later.");
    }

    @ExceptionHandler(AdminUserStatusUpdateException.class)
    ResponseEntity<ProblemDetail> handleStatusPersistenceFailure(AdminUserStatusUpdateException exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "The account status could not be persisted. Please try again later.");
    }

    @ExceptionHandler(AdminUserStatusCompensationException.class)
    ResponseEntity<ProblemDetail> handleCompensationFailure(AdminUserStatusCompensationException exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Account status synchronization failed and requires operational reconciliation.");
    }

    @ExceptionHandler({AdminSelfRoleChangeException.class, LastAdminProtectionException.class})
    ResponseEntity<ProblemDetail> handleRoleConflict(RuntimeException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(KeycloakRoleUpdateException.class)
    ResponseEntity<ProblemDetail> handleRoleProviderFailure(KeycloakRoleUpdateException exception) {
        return problem(HttpStatus.BAD_GATEWAY,
                "The identity provider could not update the account role. Please try again later.");
    }

    @ExceptionHandler(AdminUserRoleUpdateException.class)
    ResponseEntity<ProblemDetail> handleRoleAuditFailure(AdminUserRoleUpdateException exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "The account role could not be audited; the previous role was restored.");
    }

    @ExceptionHandler(AdminUserRoleCompensationException.class)
    ResponseEntity<ProblemDetail> handleRoleCompensationFailure(AdminUserRoleCompensationException exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "The account role update and compensation failed; operational reconciliation is required.");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(problem);
    }
}

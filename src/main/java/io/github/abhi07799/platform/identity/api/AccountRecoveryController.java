package io.github.abhi07799.platform.identity.api;

import io.github.abhi07799.platform.identity.application.AccountRecoveryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AccountRecoveryController {

    private static final String GENERIC_MESSAGE =
            "If the account is eligible, an email with the requested instructions will be sent.";

    private final AccountRecoveryService accountRecoveryService;

    public AccountRecoveryController(AccountRecoveryService accountRecoveryService) {
        this.accountRecoveryService = accountRecoveryService;
    }

    @Operation(summary = "Request a password reset email", security = {})
    @PostMapping("/password/forgot")
    public ResponseEntity<AccountRecoveryResponse> requestPasswordReset(
            @Valid @RequestBody AccountRecoveryRequest request) {
        accountRecoveryService.requestPasswordReset(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new AccountRecoveryResponse(GENERIC_MESSAGE));
    }

    @Operation(summary = "Resend the email verification link", security = {})
    @PostMapping("/email/verification/resend")
    public ResponseEntity<AccountRecoveryResponse> resendEmailVerification(
            @Valid @RequestBody AccountRecoveryRequest request) {
        accountRecoveryService.resendEmailVerification(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new AccountRecoveryResponse(GENERIC_MESSAGE));
    }
}

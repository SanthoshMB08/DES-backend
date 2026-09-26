package com.evaluation.prototype.common.exception;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException exception) {
        HttpStatus status = exception.getStatus();
        return ResponseEntity.status(status).body(error(status, exception.getMessage()));
    }

    // Wrong username/password on login. Spring Security throws this from
    // AuthenticationManager.authenticate() - without this handler it falls through to
    // handleUnexpected() below and looks like the server crashed.
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(error(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
    }

    // Account locked after too many failed attempts (see User.lock() / AuthService.recordFailedLogin()).
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Map<String, Object>> handleLocked(LockedException exception) {
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(error(HttpStatus.LOCKED, "This account is locked due to too many failed login attempts. Ask an administrator to unlock it."));
    }

    // Account explicitly disabled (status != ACTIVE) by an admin.
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(DisabledException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(HttpStatus.FORBIDDEN, "This account is disabled. Contact an administrator."));
    }

    // A file upload exceeded spring.servlet.multipart.max-file-size.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(error(HttpStatus.PAYLOAD_TOO_LARGE, "That file is too large to upload."));
    }

    // A concurrent update won the race (e.g. two "assign bundle" calls for the same
    // evaluator landed at the same time) - see User.version. The request itself was
    // otherwise valid, so this is safe to ask the caller to just retry.
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(
            org.springframework.orm.ObjectOptimisticLockingFailureException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(HttpStatus.CONFLICT, "This record was just updated by someone else - please refresh and try again."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(this::formatFieldError)
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return ResponseEntity.internalServerError()
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error"));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }

    private Map<String, Object> error(HttpStatus status, String message) {
        return Map.of(
                "timestamp", Instant.now(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message);
    }
}

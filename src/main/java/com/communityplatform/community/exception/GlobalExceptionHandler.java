package com.communityplatform.community.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler — catches exceptions thrown anywhere in the
 * controller or service layer and converts them into clean JSON responses.
 *
 * Without this class, Spring would return a generic white-label error page
 * or a verbose stack trace. With it, every error returns a consistent
 * JSON shape that the frontend (or Postman) can display properly.
 *
 * ── How @RestControllerAdvice works ─────────────────────────
 *  @RestControllerAdvice is a combination of:
 *    @ControllerAdvice  — applies to all @RestController classes
 *    @ResponseBody      — serialises return values to JSON automatically
 *
 *  Each @ExceptionHandler method declares which exception type it handles.
 *  When that exception is thrown anywhere in the request processing chain,
 *  Spring intercepts it here instead of propagating it to the default
 *  error handler.
 *
 * ── Error response shape ─────────────────────────────────────
 *  Every handler returns a Map<String, Object> with:
 *    timestamp  — when the error occurred
 *    status     — HTTP status code integer (e.g. 404)
 *    error      — HTTP status name (e.g. "Not Found")
 *    message    — the exception's message string
 *
 *  The validation handler adds an extra "errors" field — a map of
 *  field name → validation error message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 404 Not Found ────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ── 409 Conflict (duplicate email / username) ─────────────────
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateResourceException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // ── 403 Forbidden (not allowed to perform action) ─────────────
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // ── 401 Unauthorized (wrong email or password at login) ───────
    // BadCredentialsException is thrown by Spring Security's DaoAuthenticationProvider
    // when the password does not match the stored BCrypt hash.
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    // ── 403 Forbidden (account is disabled / soft-banned) ────────
    // DisabledException is thrown by Spring Security when UserDetails.isEnabled() = false
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(DisabledException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "This account has been disabled");
    }

    // ── 400 Bad Request (DTO validation failures) ─────────────────
    // Thrown by Spring when @Valid on a request body finds constraint violations.
    // Returns a map of fieldName → "validation message" for every failing field.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation failed");
        body.put("errors", fieldErrors);   // e.g. {"email": "must not be blank", "password": "size must be between 6 and 100"}

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }


    // ── 409 Conflict (actioning an already-closed report) ────────
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // ── 500 Internal Server Error (unexpected exceptions) ────────
    // Catch-all so the app never returns a raw Java stack trace to the client.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    // ── Shared response builder ───────────────────────────────────
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}

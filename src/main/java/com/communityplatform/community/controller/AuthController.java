package com.communityplatform.community.controller;

import com.communityplatform.community.dto.request.LoginRequest;
import com.communityplatform.community.dto.request.RegisterRequest;
import com.communityplatform.community.dto.response.AuthResponse;
import com.communityplatform.community.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AuthController — exposes the two public authentication endpoints.
 *
 * This controller is deliberately thin. It does three things per method:
 *   1. Receives and validates the request body
 *   2. Delegates to AuthService
 *   3. Returns the result with the appropriate HTTP status
 *
 * All business logic lives in AuthService — the controller knows
 * nothing about BCrypt, JWT generation, or database queries.
 *
 * ── Annotations explained ────────────────────────────────────
 *  @RestController    = @Controller + @ResponseBody
 *                       Every method's return value is serialised to JSON.
 *
 *  @RequestMapping    Sets the base path for all methods in this class.
 *                     Both endpoints start with /api/auth/...
 *
 *  @PostMapping       Maps HTTP POST to a specific sub-path.
 *
 *  @RequestBody       Tells Spring to deserialise the JSON request body
 *                     into the annotated parameter object.
 *
 *  @Valid             Triggers Bean Validation on the request body.
 *                     If any @NotBlank / @Email / @Size constraint fails,
 *                     Spring throws MethodArgumentNotValidException before
 *                     the method body even runs — GlobalExceptionHandler
 *                     catches it and returns a 400 with field errors.
 *
 *  ResponseEntity<T>  Gives us explicit control over the HTTP status code.
 *                     register returns 201 Created (a new resource was created).
 *                     login returns 200 OK (authentication, not resource creation).
 *
 * ── These endpoints are PUBLIC ───────────────────────────────
 *  SecurityConfig permits /api/auth/** without a token:
 *    .requestMatchers("/api/auth/**").permitAll()
 *  So the JWT filter runs but does nothing (no Authorization header),
 *  and the request reaches this controller as anonymous.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     *
     * Registers a new user account.
     * Returns HTTP 201 Created with an AuthResponse containing the JWT.
     *
     * Request body example:
     * {
     *   "username": "alice",
     *   "email":    "alice@example.com",
     *   "password": "secret123"
     * }
     *
     * Success response (201):
     * {
     *   "token":     "eyJhbGciOiJIUzI1NiJ9...",
     *   "tokenType": "Bearer",
     *   "userId":    1,
     *   "username":  "alice",
     *   "email":     "alice@example.com",
     *   "role":      "USER"
     * }
     *
     * Error responses:
     *   400 — validation failed (blank field, invalid email format, short password)
     *   409 — email or username already taken
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     *
     * Authenticates an existing user.
     * Returns HTTP 200 OK with an AuthResponse containing a fresh JWT.
     *
     * Request body example:
     * {
     *   "email":    "alice@example.com",
     *   "password": "secret123"
     * }
     *
     * Success response (200):
     * {
     *   "token":     "eyJhbGciOiJIUzI1NiJ9...",
     *   "tokenType": "Bearer",
     *   "userId":    1,
     *   "username":  "alice",
     *   "email":     "alice@example.com",
     *   "role":      "USER"
     * }
     *
     * Error responses:
     *   400 — validation failed (blank field, invalid email)
     *   401 — wrong email or password
     *   403 — account is disabled
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}

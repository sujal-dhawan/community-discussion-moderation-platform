package com.communityplatform.community.service;

import com.communityplatform.community.dto.request.LoginRequest;
import com.communityplatform.community.dto.request.RegisterRequest;
import com.communityplatform.community.dto.response.AuthResponse;
import com.communityplatform.community.entity.User;
import com.communityplatform.community.enums.Role;
import com.communityplatform.community.exception.DuplicateResourceException;
import com.communityplatform.community.repository.UserRepository;
import com.communityplatform.community.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService — handles user registration and login.
 *
 * This service is the first concrete piece of business logic in the project.
 * It sits between AuthController (HTTP layer) and the repositories/security
 * utilities (infrastructure layer).
 *
 * ── Dependencies injected via @RequiredArgsConstructor ───────
 *  UserRepository      — saves new users, checks for duplicates
 *  PasswordEncoder     — BCrypt-hashes the raw password before saving
 *  AuthenticationManager — Spring Security's login verification entry point
 *  JwtUtil             — generates the signed JWT token
 *
 * ── @Slf4j ────────────────────────────────────────────────────
 *  Lombok generates a SLF4J Logger field named `log`.
 *  Equivalent to writing:
 *    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
 *  Used for debug/info logging without polluting business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository       userRepository;
    private final PasswordEncoder      passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil              jwtUtil;

    // ── REGISTER ─────────────────────────────────────────────────
    /**
     * Registers a new user account and returns a JWT so the client
     * is immediately logged in after signing up (no separate login step).
     *
     * ── Step-by-step flow ────────────────────────────────────
     *  1. Check email uniqueness  → throw 409 if taken
     *  2. Check username uniqueness → throw 409 if taken
     *  3. Hash the raw password with BCrypt
     *  4. Build a User entity with role = USER (default)
     *  5. Save the User to the database
     *  6. Generate a JWT token embedding userId, email, role
     *  7. Return AuthResponse containing the token + user details
     *
     * @Transactional ensures that if the save fails (e.g. a race condition
     * on the unique constraint), the entire operation is rolled back cleanly.
     *
     * @param request validated RegisterRequest from the controller
     * @return AuthResponse with JWT token and user info
     * @throws DuplicateResourceException if email or username already exists
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.debug("Registering new user with email: {}", request.getEmail());

        // ── Step 1 & 2: Uniqueness checks ────────────────────────
        // These are application-level guards. The DB also enforces uniqueness
        // via column constraints, but checking here gives a clean error message
        // before attempting the INSERT.
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "An account with email '" + request.getEmail() + "' already exists"
            );
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException(
                    "Username '" + request.getUsername() + "' is already taken"
            );
        }

        // ── Step 3: Hash the password ─────────────────────────────
        // NEVER store a plain-text password. BCryptPasswordEncoder.encode()
        // generates a unique salt, runs the password through BCrypt, and
        // returns a 60-character string like:
        //   $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
        // The salt is embedded in the hash — no separate column needed.
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // ── Step 4: Build the User entity ────────────────────────
        // Using Lombok's @Builder — no long constructor call.
        // role defaults to USER (defined on the builder with @Builder.Default).
        // isActive defaults to true.
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .role(Role.USER)
                .isActive(true)
                .build();

        // ── Step 5: Persist to database ───────────────────────────
        // save() returns the managed entity with the generated id populated.
        // We reassign to `savedUser` so we have access to the generated id.
        User savedUser = userRepository.save(user);
        log.info("New user registered: id={}, email={}", savedUser.getId(), savedUser.getEmail());

        // ── Step 6: Generate JWT ──────────────────────────────────
        // We use savedUser (not user) because we need the DB-generated id.
        String token = jwtUtil.generateToken(
                savedUser.getEmail(),
                savedUser.getId(),
                savedUser.getRole().name()   // "USER"
        );

        // ── Step 7: Build and return the response ─────────────────
        return buildAuthResponse(token, savedUser);
    }

    // ── LOGIN ────────────────────────────────────────────────────
    /**
     * Authenticates an existing user and returns a fresh JWT.
     *
     * ── Step-by-step flow ────────────────────────────────────
     *  1. Create an unauthenticated token from email + raw password
     *  2. Pass it to AuthenticationManager.authenticate()
     *       → internally calls CustomUserDetailsService.loadUserByUsername(email)
     *       → loads the User from DB, gets the stored BCrypt hash
     *       → calls BCryptPasswordEncoder.matches(rawPassword, storedHash)
     *       → if no match → throws BadCredentialsException → 401
     *       → if match → returns a fully authenticated token
     *  3. Extract the UserDetails from the authenticated token
     *  4. Load our User entity from the DB to get id and role
     *  5. Generate a JWT token
     *  6. Return AuthResponse
     *
     * ── Why do we reload the User in step 4? ─────────────────
     *  The Authentication object from step 2 contains a Spring Security
     *  UserDetails — not our User entity. UserDetails has email and
     *  authorities, but not our userId or the full Role enum. We need
     *  userId to embed in the JWT, so we do a second DB lookup by email.
     *  This is one extra query per login — a reasonable trade-off for
     *  keeping the entity and security models separate.
     *
     * @param request validated LoginRequest from the controller
     * @return AuthResponse with JWT token and user info
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         (thrown by AuthenticationManager, caught by GlobalExceptionHandler → 401)
     */
    public AuthResponse login(LoginRequest request) {
        log.debug("Login attempt for email: {}", request.getEmail());

        // ── Step 1 & 2: Delegate authentication to Spring Security ─
        // UsernamePasswordAuthenticationToken with two args = unauthenticated.
        // AuthenticationManager.authenticate() either:
        //   a) Returns a fully authenticated token (success)
        //   b) Throws BadCredentialsException (wrong password)
        //   c) Throws UsernameNotFoundException (no such email)
        //   d) Throws DisabledException (isActive = false)
        // Cases b/c/d bubble up to GlobalExceptionHandler automatically.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),    // treated as "username" by Spring Security
                        request.getPassword()  // raw password — DaoAuthProvider BCrypt-verifies it
                )
        );

        // ── Step 3: Extract the authenticated principal ───────────
        // authentication.getPrincipal() returns the UserDetails object
        // that CustomUserDetailsService.loadUserByUsername() produced.
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // ── Step 4: Reload our User entity ───────────────────────
        // UserDetails gives us the email (via getUsername()). We look up
        // our own User entity to get the id and role for the JWT.
        // orElseThrow should never trigger here — the user passed auth —
        // but it is good practice to handle it defensively.
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException(
                        "Authenticated user not found in database — this should never happen"
                ));

        // ── Step 5: Generate JWT ──────────────────────────────────
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getId(),
                user.getRole().name()
        );

        log.info("User logged in: id={}, email={}", user.getId(), user.getEmail());

        // ── Step 6: Build and return the response ─────────────────
        return buildAuthResponse(token, user);
    }

    // ── Shared helper ─────────────────────────────────────────────
    /**
     * Builds an AuthResponse from a token and a User entity.
     * Extracted to avoid duplication between register() and login().
     */
    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}

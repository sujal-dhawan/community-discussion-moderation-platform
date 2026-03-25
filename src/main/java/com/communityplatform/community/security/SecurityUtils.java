package com.communityplatform.community.security;

import com.communityplatform.community.entity.User;
import com.communityplatform.community.exception.ResourceNotFoundException;
import com.communityplatform.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * SecurityUtils — a reusable helper that every service uses to answer:
 * "Who is the currently logged-in user?"
 *
 * ── Why this class exists ─────────────────────────────────────
 *  JwtAuthenticationFilter stores the authenticated principal in
 *  SecurityContextHolder on every request. That principal is a
 *  Spring Security UserDetails object (email + authorities) — not
 *  our User entity.
 *
 *  Every service that needs the logged-in user's id, role, or any
 *  entity field must:
 *    1. Read the email from SecurityContextHolder
 *    2. Query the DB to get the full User entity
 *
 *  Without this utility, every service would repeat those same two
 *  steps inline. Extracting them here keeps service code clean and
 *  the pattern consistent across the entire project.
 *
 * ── How to use it in a service ───────────────────────────────
 *
 *   // In any @Service class:
 *   private final SecurityUtils securityUtils;
 *
 *   public void someMethod() {
 *       User currentUser = securityUtils.getCurrentUser();
 *       // now currentUser.getId(), currentUser.getRole(), etc.
 *   }
 *
 * ── Interview talking point ──────────────────────────────────
 *  SecurityContextHolder is thread-local — each HTTP request thread
 *  has its own copy. This means getCurrentUser() is safe to call
 *  concurrently from multiple requests without synchronization.
 *  JwtAuthenticationFilter populates it at the start of the request;
 *  it is cleared automatically when the request thread finishes.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Returns the fully loaded User entity for the currently
     * authenticated user.
     *
     * Flow:
     *   1. Get the Authentication from SecurityContextHolder
     *   2. Call authentication.getName() → returns the email
     *      (because JwtAuthenticationFilter set UserDetails whose
     *       getUsername() returns the email)
     *   3. Load User from DB by email
     *
     * @return the logged-in User entity
     * @throws ResourceNotFoundException if (somehow) the email in the
     *         token does not match any user in the DB
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // authentication.getName() delegates to UserDetails.getUsername()
        // which we set to the user's email in CustomUserDetailsService
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found: " + email
                ));
    }

    /**
     * Returns just the email of the currently authenticated user,
     * without a DB lookup.
     *
     * Use this when you only need the email (e.g. for logging),
     * not the full entity — avoids the extra DB query.
     */
    public String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}

package com.communityplatform.community.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthenticationFilter — runs once on every incoming HTTP request.
 *
 * Its only job is to:
 *   1. Look for a JWT in the Authorization header
 *   2. If found, validate it
 *   3. If valid, set the authenticated user into Spring Security's context
 *   4. Always call filterChain.doFilter() to pass the request along
 *
 * After this filter runs, Spring Security's authorization rules
 * (configured in SecurityConfig) check whether the now-authenticated
 * (or still-anonymous) principal has permission to reach the endpoint.
 *
 * ── Why OncePerRequestFilter? ────────────────────────────────
 *  In a servlet container, the filter chain can theoretically be
 *  invoked multiple times per request (e.g. during error dispatches
 *  or forwards). OncePerRequestFilter guarantees this filter's logic
 *  runs exactly once per request, regardless of how many dispatches occur.
 *
 * ── Request flow diagram ─────────────────────────────────────
 *
 *  HTTP Request
 *      │
 *      ▼
 *  JwtAuthenticationFilter
 *      │
 *      ├─ No "Authorization" header?
 *      │    └─ Skip to filterChain (request stays anonymous)
 *      │
 *      ├─ Header doesn't start with "Bearer "?
 *      │    └─ Skip to filterChain (request stays anonymous)
 *      │
 *      ├─ Token present — extract email from token
 *      │
 *      ├─ SecurityContext already has auth? (already processed upstream)
 *      │    └─ Skip to filterChain
 *      │
 *      ├─ Load UserDetails from DB by email
 *      │
 *      ├─ isTokenValid() → false?
 *      │    └─ Skip to filterChain (request stays anonymous → 401 later)
 *      │
 *      └─ Token valid → build Authentication → set into SecurityContextHolder
 *           └─ filterChain continues → endpoint is reached
 *
 * ── SecurityContextHolder ────────────────────────────────────
 *  This is Spring Security's thread-local storage. Whatever
 *  Authentication object we put here is available anywhere in the
 *  request's thread — controllers, services, anywhere — via:
 *    SecurityContextHolder.getContext().getAuthentication()
 *
 *  We use this in services to get the currently logged-in user's email.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    // The standard HTTP header name for bearer tokens
    private static final String AUTH_HEADER = "Authorization";

    // Every JWT bearer token starts with this prefix
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // ── Step 1: Read the Authorization header ────────────────
        final String authHeader = request.getHeader(AUTH_HEADER);

        // If there is no Authorization header, or it does not start
        // with "Bearer ", there is no JWT to process. Pass the request
        // along — it will reach SecurityConfig as an anonymous request.
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 2: Extract the token (everything after "Bearer ") ─
        // "Bearer eyJhbGci..." → "eyJhbGci..."
        final String token = authHeader.substring(BEARER_PREFIX.length());

        // ── Step 3: Extract the email from the token ─────────────
        // extractEmail() parses the JWT. If the token is malformed,
        // it will throw a JwtException — we let that bubble up as a 400/401.
        final String email;
        try {
            email = jwtUtil.extractEmail(token);
        } catch (Exception e) {
            // Malformed token — skip authentication, let it fail downstream
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 4: Only proceed if we got an email AND the context ─
        //    is not already authenticated (avoid redundant processing)
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // ── Step 5: Load the full UserDetails from the database ─
            // This call gives Spring Security the password hash + authorities
            // so it can verify the token is for a real, active account.
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // ── Step 6: Validate the token against this UserDetails ─
            // Checks: not expired, email matches, signature valid
            if (jwtUtil.isTokenValid(token, userDetails)) {

                // ── Step 7: Build an Authentication object ───────────
                // UsernamePasswordAuthenticationToken is Spring Security's
                // standard "this user is authenticated" wrapper.
                //
                // Constructor args:
                //   principal   → the UserDetails (identity + authorities)
                //   credentials → null (we don't need the password anymore)
                //   authorities → the list of roles from UserDetails
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                // Attach request details (IP address, session id) to the
                // authentication token — useful for auditing and security events
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // ── Step 8: Store Authentication in the SecurityContext ─
                // From this point forward in the request thread, any call to
                // SecurityContextHolder.getContext().getAuthentication()
                // will return this authToken — the user is "logged in" for
                // the duration of this request.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // ── Step 9: Always pass the request to the next filter/servlet ─
        // Whether authentication succeeded or not, we always call this.
        // If the request is still anonymous, SecurityConfig will return 401.
        filterChain.doFilter(request, response);
    }
}

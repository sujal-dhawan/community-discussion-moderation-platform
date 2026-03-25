package com.communityplatform.community.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JwtUtil — responsible for three things only:
 *   1. Generating a signed JWT token after a successful login
 *   2. Parsing a token string back into its claims (email, role, expiry)
 *   3. Validating that a token is well-formed, signed correctly, and not expired
 *
 * This class has NO knowledge of HTTP requests, Spring Security contexts,
 * or the database — it is purely a utility for token math.
 *
 * ── How JWT works (interview summary) ────────────────────────
 *  A JWT has three Base64-encoded parts separated by dots:
 *    HEADER.PAYLOAD.SIGNATURE
 *
 *  Header  → algorithm used (HS256)
 *  Payload → "claims": userId, email, role, issuedAt, expiry
 *  Signature → HMAC-SHA256(header + "." + payload, secretKey)
 *
 *  The server signs the token at login. On every subsequent request,
 *  the server re-computes the signature from the received header+payload
 *  and checks it against the signature in the token. If they match,
 *  the payload has not been tampered with — the server trusts the claims
 *  without hitting the database.
 *
 *  This is why JWT enables STATELESS authentication: no session table,
 *  no cache, no DB lookup per request.
 *
 * ── Secret key sizing ────────────────────────────────────────
 *  HS256 requires a minimum 256-bit (32-byte) key.
 *  The secret in application.properties is a plain string — we
 *  convert it to bytes and wrap it in an HMAC-SHA key via Keys.hmacShaKeyFor().
 *  If the string is shorter than 32 bytes the library will throw at startup.
 *
 * ── @Value injection ─────────────────────────────────────────
 *  @Value("${app.jwt.secret}") reads the property from application.properties.
 *  Spring injects it at bean creation time — we never hardcode secrets.
 */
@Component
public class JwtUtil {

    // Injected from application.properties — app.jwt.secret
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // Injected from application.properties — app.jwt.expiration (milliseconds)
    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    // ── Key builder ──────────────────────────────────────────────
    /**
     * Converts the plain-text secret string into a cryptographic SecretKey
     * suitable for HMAC-SHA256 signing.
     *
     * Called internally every time we need to sign or verify — we do NOT
     * cache it as a field because @Value is injected AFTER the constructor runs.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Token generation ─────────────────────────────────────────
    /**
     * Builds and signs a JWT token for a successfully authenticated user.
     *
     * Claims embedded in the token:
     *   subject  → the user's email (used to reload the user on each request)
     *   userId   → Long id — useful so services don't need a second DB lookup
     *   role     → "USER" or "MODERATOR" — read by Spring Security for authorization
     *   issuedAt → current timestamp
     *   expiry   → issuedAt + jwtExpirationMs
     *
     * @param email  the authenticated user's email
     * @param userId the authenticated user's database id
     * @param role   the authenticated user's role as a string ("USER" / "MODERATOR")
     * @return signed JWT string — returned to the client in the AuthResponse
     */
    public String generateToken(String email, Long userId, String role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)                          // standard "sub" claim
                .claim("userId", userId)                 // custom claim
                .claim("role", role)                     // custom claim
                .issuedAt(now)                           // standard "iat" claim
                .expiration(expiry)                      // standard "exp" claim
                .signWith(getSigningKey())               // signs with HS256
                .compact();                              // serialises to the dot-separated string
    }

    // ── Claims extraction ────────────────────────────────────────
    /**
     * Parses the token and returns the full Claims object.
     *
     * Claims is a Map-like object containing all the key-value pairs
     * embedded in the token payload (subject, userId, role, expiry, etc.).
     *
     * This method is the single parse point — the helpers below all
     * delegate here so the token is only parsed once per call.
     *
     * Throws JwtException (or a subclass) if the token is:
     *   - malformed (not a valid JWT string)
     *   - signature mismatch (tampered payload)
     *   - expired
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())   // sets the key used to verify the signature
                .build()
                .parseSignedClaims(token)      // parses AND verifies in one step
                .getPayload();                 // returns just the claims map
    }

    /**
     * Extracts the email (subject) from the token.
     * Used by JwtAuthenticationFilter to know WHICH user to load from the DB.
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts the userId custom claim from the token.
     * Useful in services that need the current user's id without a DB call.
     */
    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    /**
     * Extracts the role custom claim from the token.
     * Returns "USER" or "MODERATOR" as a plain string.
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // ── Token validation ─────────────────────────────────────────
    /**
     * Returns true if the token:
     *   1. Parses without a JwtException (well-formed, signature valid)
     *   2. Is not expired
     *   3. The email in the token matches the UserDetails loaded from the DB
     *
     * The UserDetails check (#3) is a safety guard: if a user changes their
     * email after receiving a token, the old token would still parse correctly
     * but the email would no longer match — this call rejects it.
     *
     * @param token       the raw JWT string from the Authorization header
     * @param userDetails the UserDetails loaded from DB by CustomUserDetailsService
     * @return true if the token is valid for this user
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String emailFromToken = extractEmail(token);
            boolean notExpired    = !extractAllClaims(token).getExpiration().before(new Date());
            boolean emailMatches  = emailFromToken.equals(userDetails.getUsername());
            return notExpired && emailMatches;
        } catch (JwtException | IllegalArgumentException e) {
            // Any parsing failure — malformed, tampered, expired — returns false
            return false;
        }
    }
}

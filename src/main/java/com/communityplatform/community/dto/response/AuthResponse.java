package com.communityplatform.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AuthResponse — the response body for both /register and /login.
 *
 * Returned to the client after a successful authentication operation.
 * The client stores the token (typically in localStorage or memory)
 * and sends it with every subsequent request as:
 *   Authorization: Bearer <token>
 *
 * ── Why include userId, username, role? ──────────────────────
 *  The JWT token already contains these values as claims, and a
 *  client COULD decode the token itself (the payload is Base64,
 *  not encrypted). However, returning them explicitly in the
 *  response body is more convenient — the client doesn't need
 *  to decode the token just to display "Welcome, Alice" or to
 *  decide whether to show the moderator dashboard.
 *
 * ── tokenType ────────────────────────────────────────────────
 *  Always "Bearer". Included so the client knows exactly how to
 *  format the Authorization header without hardcoding "Bearer"
 *  on their side. Consistent with the OAuth2 convention.
 *
 * ── What is NOT in AuthResponse ─────────────────────────────
 *  - password (obviously — never send back)
 *  - passwordHash (never send back)
 *  - isActive (internal admin concern)
 *  - createdAt (not needed at login time)
 *
 * ── @Builder ─────────────────────────────────────────────────
 *  Lombok's @Builder lets us construct this object cleanly in
 *  AuthService without a long constructor call:
 *
 *    return AuthResponse.builder()
 *        .token(jwt)
 *        .userId(user.getId())
 *        .username(user.getUsername())
 *        .email(user.getEmail())
 *        .role(user.getRole().name())
 *        .build();
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long userId;
    private String username;
    private String email;
    private String role;       // "USER" or "MODERATOR" — plain string for easy client use
}

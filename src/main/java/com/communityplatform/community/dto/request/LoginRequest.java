package com.communityplatform.community.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LoginRequest — the request body for POST /api/auth/login.
 *
 * Intentionally minimal — login only needs email and password.
 * Username is not needed for login in this design; we identify
 * users by email because it is unique and cannot be changed
 * as easily as a username.
 *
 * No @Size on password here — we don't want to reveal constraints
 * to a potential attacker trying to narrow down valid passwords.
 * The only validation needed is "not blank".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}

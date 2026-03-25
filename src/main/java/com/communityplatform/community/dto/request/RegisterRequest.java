package com.communityplatform.community.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RegisterRequest — the request body for POST /api/auth/register.
 *
 * ── What is a DTO? ────────────────────────────────────────────
 *  DTO = Data Transfer Object. It is a simple class whose only job
 *  is to carry data between the HTTP layer (controller) and the
 *  business logic layer (service).
 *
 *  We do NOT use the User entity directly as the request body because:
 *    1. The entity has fields the client should never send (id, role,
 *       isActive, createdAt, collections of posts/comments etc.)
 *    2. Exposing the entity directly couples the API contract to the
 *       database schema — a change in one breaks the other.
 *    3. Validation annotations belong on the DTO, not the entity.
 *
 * ── Validation annotations ───────────────────────────────────
 *  These come from jakarta.validation (the Bean Validation spec),
 *  pulled in by spring-boot-starter-validation in pom.xml.
 *
 *  @NotBlank  — field must not be null, empty, or whitespace-only
 *  @Email     — field must match a valid email format (x@x.x)
 *  @Size      — enforces min/max character length
 *
 *  These annotations do nothing on their own — they are activated
 *  by adding @Valid to the controller method parameter. If any
 *  constraint fails, Spring throws MethodArgumentNotValidException,
 *  which our GlobalExceptionHandler converts to a 400 JSON response.
 *
 * ── Lombok ───────────────────────────────────────────────────
 *  @Data           = @Getter + @Setter + @ToString + @EqualsAndHashCode
 *  @NoArgsConstructor  = public RegisterRequest() {}
 *  @AllArgsConstructor = public RegisterRequest(username, email, password) {}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;
}

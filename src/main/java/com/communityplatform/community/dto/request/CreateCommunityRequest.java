package com.communityplatform.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateCommunityRequest — request body for POST /api/communities.
 *
 * Only MODERATOR role can reach this endpoint (enforced in SecurityConfig
 * and reinforced by @PreAuthorize in CommunityController).
 *
 * The `description` field is optional — a community can be created with
 * just a name. This matches how Reddit communities work (description can
 * be added later). We do not have an "update community" endpoint in this
 * simplified design, but that would accept the same shape.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommunityRequest {

    @NotBlank(message = "Community name is required")
    @Size(min = 3, max = 100, message = "Community name must be between 3 and 100 characters")
    private String name;

    // Optional — no @NotBlank, just a max length guard
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}

package com.communityplatform.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreatePostRequest — request body for POST /api/posts.
 *
 * The client sends three fields:
 *   - communityId  → which community to post in
 *   - title        → the post heading
 *   - content      → the post body
 *
 * ── Why communityId is in the body, not the URL ──────────────
 *  Two common designs exist:
 *    a) POST /api/communities/{communityId}/posts  (nested URL)
 *    b) POST /api/posts with communityId in body   (flat URL)
 *
 *  This project uses (b). It keeps the controller structure flat
 *  and mirrors how many real APIs work (e.g. sending a FK in the
 *  payload). Option (a) is equally valid — worth mentioning in an
 *  interview as a deliberate trade-off.
 *
 * ── @NotNull vs @NotBlank ─────────────────────────────────────
 *  @NotNull  — rejects null only (empty string passes)
 *  @NotBlank — rejects null, empty string, and whitespace-only
 *  communityId is a Long, so @NotNull is correct (no "blank" concept).
 *  title and content are Strings, so @NotBlank is correct.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {

    @NotNull(message = "Community ID is required")
    private Long communityId;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(min = 10, max = 10000, message = "Content must be between 10 and 10,000 characters")
    private String content;
}

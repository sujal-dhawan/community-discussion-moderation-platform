package com.communityplatform.community.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateReviewRequest — request body for POST /api/reviews.
 *
 * Reviews are standalone — not tied to a Post or Community.
 * The client describes what is being reviewed (subjectName + subjectType)
 * and provides the text and a 1–5 star rating.
 *
 * ── @Min / @Max ───────────────────────────────────────────────
 *  These come from jakarta.validation and enforce numeric range.
 *  The entity accepts any Integer; the DTO is the right place to
 *  reject invalid ratings before they reach the database.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {

    @NotBlank(message = "Subject name is required")
    @Size(max = 200, message = "Subject name must not exceed 200 characters")
    private String subjectName;    // e.g. "Taj Mahal Hotel"

    @NotBlank(message = "Subject type is required")
    @Size(max = 50, message = "Subject type must not exceed 50 characters")
    private String subjectType;    // e.g. "PLACE", "RESTAURANT", "ITEM"

    @NotBlank(message = "Review content is required")
    @Size(min = 10, max = 5000, message = "Review must be between 10 and 5000 characters")
    private String content;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;
}

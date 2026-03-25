package com.communityplatform.community.dto.request;

import com.communityplatform.community.enums.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateReportRequest — request body for POST /api/reports.
 *
 * A user reports a specific piece of content by providing:
 *   contentType  — which kind of content (POST, COMMENT, or REVIEW)
 *   contentId    — the numeric ID of that content
 *   reason       — a free-text explanation of why it's being reported
 *
 * Jackson automatically deserialises "POST", "COMMENT", "REVIEW"
 * into the ContentType enum. Any other value produces a 400.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequest {

    @NotNull(message = "Content type is required (POST, COMMENT, or REVIEW)")
    private ContentType contentType;

    @NotNull(message = "Content ID is required")
    private Long contentId;

    @NotBlank(message = "Reason is required")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    private String reason;
}

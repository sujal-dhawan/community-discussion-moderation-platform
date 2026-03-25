package com.communityplatform.community.dto.response;

import com.communityplatform.community.enums.ContentType;
import com.communityplatform.community.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ReportResponse — returned for report and moderation endpoints.
 *
 * Includes all lifecycle fields:
 *   reporterUsername   — who filed the report
 *   resolvedByUsername — who actioned it (null until resolved/dismissed)
 *   status             — PENDING, RESOLVED, or DISMISSED
 *   resolvedAt         — timestamp of resolution (null until then)
 *
 * The contentType + contentId together identify the reported item.
 * The moderator can use these to fetch and review the content before
 * deciding to delete or dismiss.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {

    private Long        id;
    private ContentType contentType;
    private Long        contentId;
    private String      reason;
    private ReportStatus status;

    // Reporter info
    private Long   reporterId;
    private String reporterUsername;

    // Resolver info — null while PENDING
    private Long   resolvedById;
    private String resolvedByUsername;

    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}

package com.communityplatform.community.entity;

import com.communityplatform.community.enums.ContentType;
import com.communityplatform.community.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a moderation report filed by a user against a piece of content.
 *
 * ── Relationships ────────────────────────────────────────────
 *  Many Reports → One User (reporter)
 *  Many Reports → One User (resolvedBy — nullable moderator)
 *
 * ── Interview notes ──────────────────────────────────────────
 *  - Report uses a POLYMORPHIC ASSOCIATION pattern via two fields:
 *       contentType  — which entity type is being reported (POST/COMMENT/REVIEW)
 *       contentId    — the primary key of that entity
 *    This means ONE reports table covers all three reportable content types
 *    without needing three separate FK columns (post_id, comment_id, review_id)
 *    most of which would always be NULL.
 *
 *  - There are TWO separate ManyToOne relationships to User:
 *       reporter    — the user who filed the report (always set)
 *       resolvedBy  — the moderator who acted on it (nullable until resolved)
 *    Each has its own @JoinColumn with a descriptive column name.
 *
 *  - `status` defaults to PENDING. It transitions to RESOLVED (moderator
 *    took action) or DISMISSED (moderator found no violation). This makes
 *    the moderation queue easy to query: WHERE status = 'PENDING'.
 *
 *  - `resolvedAt` is set in ModerationService at the time of action.
 *    It is nullable because a new report has not been resolved yet.
 *
 *  - No `isDeleted` flag here — reports are audit records and should
 *    never be soft-deleted. Status transitions are the lifecycle.
 */
@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── What is being reported (polymorphic association) ─────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ContentType contentType;          // POST, COMMENT, or REVIEW

    @Column(nullable = false)
    private Long contentId;                   // the ID of the reported item

    // ── Report details ───────────────────────────────────────
    @Column(nullable = false, length = 500)
    private String reason;                    // free-text reason from the reporter

    // ── Lifecycle status ─────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    // ── Audit ────────────────────────────────────────────────
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column                                   // nullable — set when status changes
    private LocalDateTime resolvedAt;

    // ── Relationships ────────────────────────────────────────

    // The user who filed this report.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    // The moderator who resolved or dismissed this report.
    // nullable = true (default) because new reports haven't been reviewed yet.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;
}

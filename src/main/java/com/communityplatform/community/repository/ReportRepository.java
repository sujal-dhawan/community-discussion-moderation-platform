package com.communityplatform.community.repository;

import com.communityplatform.community.entity.Report;
import com.communityplatform.community.enums.ContentType;
import com.communityplatform.community.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for the Report entity.
 *
 * This is the backbone of the moderation queue. Every method here
 * is used exclusively by ModerationService (MODERATOR role only).
 *
 * ── Custom methods ───────────────────────────────────────────
 *
 *  findByStatus(status, pageable)
 *    → SELECT * FROM reports WHERE status = ?
 *
 *    The primary moderation queue query.
 *    Moderators call this with ReportStatus.PENDING to see what
 *    needs action. Can also be called with RESOLVED or DISMISSED
 *    to review historical decisions.
 *
 *  findByContentTypeAndStatus(contentType, status, pageable)
 *    → SELECT * FROM reports WHERE content_type = ? AND status = ?
 *
 *    Filtered moderation queue — e.g. "show me only pending reports
 *    about POSTs". Useful when a moderator wants to triage by type.
 *
 *  existsByReporterIdAndContentTypeAndContentId(reporterId, contentType, contentId)
 *    → SELECT 1 FROM reports
 *      WHERE reporter_id = ? AND content_type = ? AND content_id = ?
 *      LIMIT 1
 *
 *    Prevents duplicate reports. Before saving a new report,
 *    ReportService calls this to check whether this user has already
 *    reported this exact piece of content. Returns boolean — no need
 *    to load the full Report object.
 *
 *  countByStatus(status)
 *    → SELECT COUNT(*) FROM reports WHERE status = ?
 *
 *    Returns the number of pending reports — useful for a dashboard
 *    badge showing moderators how many items need attention.
 *
 * ── Interview talking point ──────────────────────────────────
 *  Q: "Why not use a JOIN to fetch the actual reported content?"
 *  A: Because contentId is a polymorphic reference — it could point
 *     to posts, comments, OR reviews. SQL JOINs require a concrete
 *     table. In ModerationService, we switch on contentType first,
 *     THEN call the appropriate repository (PostRepository,
 *     CommentRepository, or ReviewRepository) with contentId.
 *     Two separate queries, but clean and explicit.
 *
 *  Q: "Why is there no isDeleted on Report?"
 *  A: Reports are audit records. Deleting them — even softly — would
 *     destroy the moderation history. Instead, status = DISMISSED
 *     communicates "no action needed" without removing the record.
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // ── Main moderation queue ───────────────────────────────────
    // Pass ReportStatus.PENDING to get the active queue
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    // ── Filtered queue by content type ─────────────────────────
    // e.g. show only PENDING reports about COMMENTs
    Page<Report> findByContentTypeAndStatus(
            ContentType contentType,
            ReportStatus status,
            Pageable pageable
    );

    // ── Duplicate report guard ──────────────────────────────────
    // Returns true if this user already reported this exact content
    boolean existsByReporterIdAndContentTypeAndContentId(
            Long reporterId,
            ContentType contentType,
            Long contentId
    );

    // ── Dashboard count ─────────────────────────────────────────
    // How many reports of a given status exist right now?
    long countByStatus(ReportStatus status);
}

package com.communityplatform.community.service;

import com.communityplatform.community.dto.request.CreateReportRequest;
import com.communityplatform.community.dto.response.ReportResponse;
import com.communityplatform.community.entity.Comment;
import com.communityplatform.community.entity.Post;
import com.communityplatform.community.entity.Report;
import com.communityplatform.community.entity.Review;
import com.communityplatform.community.entity.User;
import com.communityplatform.community.enums.ContentType;
import com.communityplatform.community.enums.ReportStatus;
import com.communityplatform.community.exception.DuplicateResourceException;
import com.communityplatform.community.exception.ResourceNotFoundException;
import com.communityplatform.community.repository.CommentRepository;
import com.communityplatform.community.repository.PostRepository;
import com.communityplatform.community.repository.ReportRepository;
import com.communityplatform.community.repository.ReviewRepository;
import com.communityplatform.community.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * ReportService — handles both sides of moderation:
 *   1. Users filing reports against content
 *   2. Moderators viewing, resolving, and dismissing those reports
 *
 * ── Polymorphic content validation ───────────────────────────
 *  The Report entity uses contentType + contentId rather than three
 *  separate FK columns. When a report is filed, we validate that the
 *  content actually exists by switching on contentType and calling the
 *  appropriate repository. This prevents reports targeting non-existent
 *  or already-deleted content.
 *
 * ── Moderation actions ────────────────────────────────────────
 *  RESOLVE — moderator deletes the reported content (soft-delete)
 *            and marks the report RESOLVED.
 *  DISMISS — moderator finds no violation; content stays, report
 *            marked DISMISSED.
 *
 * ── Duplicate report prevention ──────────────────────────────
 *  A user cannot report the same piece of content twice.
 *  ReportRepository.existsByReporterIdAndContentTypeAndContentId()
 *  provides a fast existence check before INSERT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository  reportRepository;
    private final PostRepository    postRepository;
    private final CommentRepository commentRepository;
    private final ReviewRepository  reviewRepository;
    private final SecurityUtils     securityUtils;

    // ── FILE A REPORT ─────────────────────────────────────────────
    /**
     * Authenticated user files a report against a post, comment, or review.
     *
     * Steps:
     *  1. Verify the reported content actually exists
     *  2. Prevent duplicate reports from the same user
     *  3. Save the Report with status = PENDING
     */
    @Transactional
    public ReportResponse createReport(CreateReportRequest request) {

        // Step 1: Validate the content exists and is not deleted
        validateContentExists(request.getContentType(), request.getContentId());

        User reporter = securityUtils.getCurrentUser();

        // Step 2: Duplicate guard — one report per user per content item
        if (reportRepository.existsByReporterIdAndContentTypeAndContentId(
                reporter.getId(), request.getContentType(), request.getContentId())) {
            throw new DuplicateResourceException(
                    "You have already reported this content"
            );
        }

        // Step 3: Save with PENDING status (default set on entity via @Builder.Default)
        Report report = Report.builder()
                .contentType(request.getContentType())
                .contentId(request.getContentId())
                .reason(request.getReason())
                .reporter(reporter)
                .build();

        Report saved = reportRepository.save(report);
        log.info("Report filed: id={}, contentType={}, contentId={}, reporterId={}",
                saved.getId(), saved.getContentType(), saved.getContentId(), reporter.getId());

        return toResponse(saved);
    }

    // ── MODERATOR: VIEW PENDING REPORTS ──────────────────────────
    /**
     * Returns a paginated page of reports filtered by status.
     * Moderator calls this with status=PENDING to see the active queue.
     * Can also use RESOLVED or DISMISSED to review past decisions.
     */
    @Transactional(readOnly = true)
    public Page<ReportResponse> getReportsByStatus(ReportStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return reportRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    // ── MODERATOR: GET SINGLE REPORT ─────────────────────────────
    @Transactional(readOnly = true)
    public ReportResponse getReportById(Long id) {
        return toResponse(findReportOrThrow(id));
    }

    // ── MODERATOR: RESOLVE (delete content + close report) ───────
    /**
     * Resolves a report by soft-deleting the reported content
     * and marking the report as RESOLVED.
     *
     * Both operations happen in the same @Transactional boundary —
     * if either fails, both roll back.
     */
    @Transactional
    public ReportResponse resolveReport(Long reportId) {
        Report report = findPendingReportOrThrow(reportId);
        User moderator = securityUtils.getCurrentUser();

        // Soft-delete the reported content via the appropriate repository
        softDeleteContent(report.getContentType(), report.getContentId());

        // Mark the report as resolved
        report.setStatus(ReportStatus.RESOLVED);
        report.setResolvedBy(moderator);
        report.setResolvedAt(LocalDateTime.now());
        reportRepository.save(report);

        log.info("Report resolved: id={}, by moderatorId={}", reportId, moderator.getId());
        return toResponse(report);
    }

    // ── MODERATOR: DISMISS (no action on content) ────────────────
    /**
     * Dismisses a report without taking action on the content.
     * The content remains visible; the report is closed as DISMISSED.
     */
    @Transactional
    public ReportResponse dismissReport(Long reportId) {
        Report report = findPendingReportOrThrow(reportId);
        User moderator = securityUtils.getCurrentUser();

        report.setStatus(ReportStatus.DISMISSED);
        report.setResolvedBy(moderator);
        report.setResolvedAt(LocalDateTime.now());
        reportRepository.save(report);

        log.info("Report dismissed: id={}, by moderatorId={}", reportId, moderator.getId());
        return toResponse(report);
    }

    // ── MODERATOR: PENDING REPORT COUNT ──────────────────────────
    /** Returns the count of PENDING reports — useful for a dashboard badge. */
    @Transactional(readOnly = true)
    public long getPendingReportCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    // ── Private helpers ───────────────────────────────────────────

    /**
     * Validates that the content being reported exists and is not already deleted.
     * Switches on ContentType enum to call the correct repository.
     * Throws ResourceNotFoundException if the content is missing.
     */
    private void validateContentExists(ContentType type, Long contentId) {
        switch (type) {
            case POST -> postRepository.findById(contentId)
                    .filter(p -> !p.getIsDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Post not found with id: " + contentId));

            case COMMENT -> commentRepository.findById(contentId)
                    .filter(c -> !c.getIsDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Comment not found with id: " + contentId));

            case REVIEW -> reviewRepository.findById(contentId)
                    .filter(r -> !r.getIsDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Review not found with id: " + contentId));
        }
    }

    /**
     * Soft-deletes the reported content by setting isDeleted=true.
     * Called when a moderator resolves a report.
     */
    private void softDeleteContent(ContentType type, Long contentId) {
        switch (type) {
            case POST -> {
                Post post = postRepository.findById(contentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + contentId));
                post.setIsDeleted(true);
                postRepository.save(post);
            }
            case COMMENT -> {
                Comment comment = commentRepository.findById(contentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + contentId));
                comment.setIsDeleted(true);
                commentRepository.save(comment);
            }
            case REVIEW -> {
                Review review = reviewRepository.findById(contentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + contentId));
                review.setIsDeleted(true);
                reviewRepository.save(review);
            }
        }
    }

    private Report findReportOrThrow(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
    }

    private Report findPendingReportOrThrow(Long id) {
        Report report = findReportOrThrow(id);
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new IllegalStateException(
                    "Report " + id + " is already " + report.getStatus() + " and cannot be actioned again"
            );
        }
        return report;
    }

    // ── Entity → DTO ─────────────────────────────────────────────
    private ReportResponse toResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .contentType(report.getContentType())
                .contentId(report.getContentId())
                .reason(report.getReason())
                .status(report.getStatus())
                .reporterId(report.getReporter().getId())
                .reporterUsername(report.getReporter().getUsername())
                .resolvedById(report.getResolvedBy() != null ? report.getResolvedBy().getId() : null)
                .resolvedByUsername(report.getResolvedBy() != null ? report.getResolvedBy().getUsername() : null)
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }
}

package com.communityplatform.community.controller;

import com.communityplatform.community.dto.request.CreateReportRequest;
import com.communityplatform.community.dto.response.ReportResponse;
import com.communityplatform.community.enums.ReportStatus;
import com.communityplatform.community.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ReportController — handles both user-facing and moderator-facing operations.
 *
 * This controller is split into two logical sections:
 *
 * ── USER section (any authenticated user) ─────────────────────
 *  POST  /api/reports                    → file a report against content
 *
 * ── MODERATOR section (@PreAuthorize enforced) ────────────────
 *  GET   /api/reports                    → list reports filtered by status
 *  GET   /api/reports/{id}               → view a single report
 *  GET   /api/reports/count/pending      → count of pending reports
 *  PATCH /api/reports/{id}/resolve       → delete content + close report
 *  PATCH /api/reports/{id}/dismiss       → close report without action
 *
 * ── Why one controller instead of two? ───────────────────────
 *  Both concerns operate on the Report entity and share ReportService.
 *  Splitting into ReportController + ModerationController would
 *  require the service to be injected in two places for no benefit.
 *  Access control is enforced per-method via @PreAuthorize, which
 *  is clear, explicit, and testable.
 *
 * ── PATCH vs DELETE for resolve/dismiss ──────────────────────
 *  We use PATCH (partial update) rather than DELETE because we are
 *  not removing the Report row — we are changing its status field.
 *  PATCH is semantically correct: "update part of this resource".
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ════════════════════════════════════════════
    // USER ENDPOINTS
    // ════════════════════════════════════════════

    /**
     * POST /api/reports
     * Any authenticated user files a report against a post, comment, or review.
     *
     * Request body:
     * {
     *   "contentType": "POST",
     *   "contentId":   1,
     *   "reason":      "This post contains spam links."
     * }
     *
     * Returns 201 Created with the saved report.
     * Returns 404 if the reported content does not exist.
     * Returns 409 if the user has already reported this exact content.
     */
    @PostMapping
    public ResponseEntity<ReportResponse> createReport(
            @Valid @RequestBody CreateReportRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.createReport(request));
    }

    // ════════════════════════════════════════════
    // MODERATOR ENDPOINTS
    // ════════════════════════════════════════════

    /**
     * GET /api/reports?status=PENDING&page=0&size=20
     * Returns paginated reports filtered by status.
     *
     * Access: MODERATOR only
     *
     * status defaults to PENDING (the active moderation queue).
     * Pass RESOLVED or DISMISSED to review past decisions.
     *
     * Example: GET /api/reports?status=PENDING&page=0&size=10
     */
    @GetMapping
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Page<ReportResponse>> getReports(
            @RequestParam(defaultValue = "PENDING") ReportStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(reportService.getReportsByStatus(status, page, size));
    }

    /**
     * GET /api/reports/{id}
     * Returns a single report by its id.
     * Moderators fetch this to read the full reason and content details
     * before deciding to resolve or dismiss.
     *
     * Access: MODERATOR only
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ReportResponse> getReportById(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getReportById(id));
    }

    /**
     * GET /api/reports/count/pending
     * Returns the count of PENDING reports as a simple JSON object.
     * Used for a dashboard badge: { "pendingCount": 5 }
     *
     * Access: MODERATOR only
     */
    @GetMapping("/count/pending")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        long count = reportService.getPendingReportCount();
        return ResponseEntity.ok(Map.of("pendingCount", count));
    }

    /**
     * PATCH /api/reports/{id}/resolve
     * Moderator resolves a report:
     *   1. Soft-deletes the reported content (post/comment/review)
     *   2. Sets report status = RESOLVED and records who resolved it
     *
     * Access: MODERATOR only
     * Returns 200 OK with the updated ReportResponse.
     * Returns 409 if the report is already resolved or dismissed.
     */
    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ReportResponse> resolveReport(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.resolveReport(id));
    }

    /**
     * PATCH /api/reports/{id}/dismiss
     * Moderator dismisses a report — no action taken on the content.
     * Sets report status = DISMISSED and records who dismissed it.
     *
     * Access: MODERATOR only
     * Returns 200 OK with the updated ReportResponse.
     * Returns 409 if the report is already resolved or dismissed.
     */
    @PatchMapping("/{id}/dismiss")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ReportResponse> dismissReport(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.dismissReport(id));
    }
}

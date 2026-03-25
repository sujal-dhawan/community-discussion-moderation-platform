package com.communityplatform.community.controller;

import com.communityplatform.community.dto.request.CreateReviewRequest;
import com.communityplatform.community.dto.response.ReviewResponse;
import com.communityplatform.community.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ReviewController — HTTP layer for the Review module.
 *
 * ── Endpoints ────────────────────────────────────────────────
 *  POST   /api/reviews                            [Authenticated]
 *  GET    /api/reviews?page=0&size=10             [Public]
 *  GET    /api/reviews/{id}                       [Public]
 *  GET    /api/reviews/type/{subjectType}?page=0  [Public]
 *  DELETE /api/reviews/{id}                       [Owner or Moderator]
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** POST /api/reviews — create a new review. Returns 201 Created. */
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(request));
    }

    /** GET /api/reviews — paginated list of all live reviews, newest first. */
    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> getAllReviews(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(reviewService.getAllReviews(page, size));
    }

    /** GET /api/reviews/{id} — single review by id. Returns 404 if not found. */
    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getReviewById(id));
    }

    /**
     * GET /api/reviews/type/{subjectType}
     * Filters reviews by subject type, e.g. /api/reviews/type/PLACE
     */
    @GetMapping("/type/{subjectType}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByType(
            @PathVariable String subjectType,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(reviewService.getReviewsBySubjectType(subjectType, page, size));
    }

    /** DELETE /api/reviews/{id} — soft-delete. Returns 204 No Content. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}

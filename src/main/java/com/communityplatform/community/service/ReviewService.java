package com.communityplatform.community.service;

import com.communityplatform.community.dto.request.CreateReviewRequest;
import com.communityplatform.community.dto.response.ReviewResponse;
import com.communityplatform.community.entity.Review;
import com.communityplatform.community.entity.User;
import com.communityplatform.community.enums.Role;
import com.communityplatform.community.exception.ResourceNotFoundException;
import com.communityplatform.community.exception.UnauthorizedException;
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

/**
 * ReviewService — business logic for the Review module.
 *
 * Reviews are standalone — not linked to posts or communities.
 * Think of them as a platform-wide review section (like Google Maps
 * reviews) sitting alongside the discussion (Reddit-style) area.
 *
 * ── Four operations ───────────────────────────────────────────
 *  createReview   — authenticated user writes a review
 *  getAllReviews   — paginated list of all live reviews
 *  getReviewById  — fetch a single review
 *  deleteReview   — soft-delete by author or moderator
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final SecurityUtils    securityUtils;

    // ── CREATE ────────────────────────────────────────────────────
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        User author = securityUtils.getCurrentUser();

        Review review = Review.builder()
                .subjectName(request.getSubjectName())
                .subjectType(request.getSubjectType().toUpperCase()) // normalise to upper case
                .content(request.getContent())
                .rating(request.getRating())
                .user(author)
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Review created: id={}, subject='{}', authorId={}",
                saved.getId(), saved.getSubjectName(), author.getId());

        return toResponse(saved);
    }

    // ── READ: ALL (paginated) ─────────────────────────────────────
    /**
     * Returns all non-deleted reviews, newest first.
     * Public — no authentication required.
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAllReviews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reviewRepository.findByIsDeletedFalse(pageable).map(this::toResponse);
    }

    // ── READ: SINGLE ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ReviewResponse getReviewById(Long id) {
        Review review = reviewRepository.findById(id)
                .filter(r -> !r.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review not found with id: " + id
                ));
        return toResponse(review);
    }

    // ── READ: BY SUBJECT TYPE ─────────────────────────────────────
    /**
     * Filters reviews by subject type (e.g. "PLACE", "RESTAURANT").
     * Useful for a tabbed UI: "All | Places | Restaurants | Items".
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsBySubjectType(String subjectType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reviewRepository
                .findBySubjectTypeAndIsDeletedFalse(subjectType.toUpperCase(), pageable)
                .map(this::toResponse);
    }

    // ── DELETE (soft) ─────────────────────────────────────────────
    @Transactional
    public void deleteReview(Long id) {
        Review review = reviewRepository.findById(id)
                .filter(r -> !r.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review not found with id: " + id
                ));

        User currentUser = securityUtils.getCurrentUser();
        boolean isOwner     = review.getUser().getId().equals(currentUser.getId());
        boolean isModerator = currentUser.getRole() == Role.MODERATOR;

        if (!isOwner && !isModerator) {
            throw new UnauthorizedException("You do not have permission to delete this review");
        }

        review.setIsDeleted(true);
        reviewRepository.save(review);
        log.info("Review soft-deleted: id={}, by userId={}", id, currentUser.getId());
    }

    // ── Entity → DTO ─────────────────────────────────────────────
    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .subjectName(review.getSubjectName())
                .subjectType(review.getSubjectType())
                .content(review.getContent())
                .rating(review.getRating())
                .authorId(review.getUser().getId())
                .authorUsername(review.getUser().getUsername())
                .createdAt(review.getCreatedAt())
                .build();
    }
}

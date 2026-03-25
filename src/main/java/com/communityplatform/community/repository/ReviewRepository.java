package com.communityplatform.community.repository;

import com.communityplatform.community.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the Review entity.
 *
 * ── Custom methods ───────────────────────────────────────────
 *
 *  findByIsDeletedFalse(pageable)
 *    → SELECT * FROM reviews WHERE is_deleted = false
 *
 *    The main listing query — returns all non-deleted reviews,
 *    paginated. Used for the public review feed.
 *
 *  findByUserIdAndIsDeletedFalse(userId, pageable)
 *    → SELECT * FROM reviews WHERE user_id = ? AND is_deleted = false
 *
 *    Fetches all live reviews written by a specific user.
 *    Used for a user's profile / review history page.
 *
 *  findBySubjectTypeAndIsDeletedFalse(subjectType, pageable)
 *    → SELECT * FROM reviews WHERE subject_type = ? AND is_deleted = false
 *
 *    Filters reviews by category — e.g. show only "PLACE" reviews
 *    or only "RESTAURANT" reviews. subjectType is a plain String
 *    field so callers pass "PLACE", "ITEM", etc.
 *
 * ── Interview talking point ──────────────────────────────────
 *  Review is intentionally standalone — it has no FK to Community
 *  or Post. It represents a separate feature area of the platform
 *  (think Google Maps reviews vs Reddit threads — different things).
 *
 *  Because there is no subjects table, subjectName and subjectType
 *  are free-text fields. In a production system you might later
 *  extract a Subject entity and add a proper FK. But for this
 *  project, keeping it as strings is the right trade-off between
 *  simplicity and demonstrating the concept.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Public review feed — all live reviews, paginated
    Page<Review> findByIsDeletedFalse(Pageable pageable);

    // A user's personal review history
    Page<Review> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    // Filter reviews by category (e.g. "PLACE", "ITEM", "RESTAURANT")
    Page<Review> findBySubjectTypeAndIsDeletedFalse(String subjectType, Pageable pageable);
}

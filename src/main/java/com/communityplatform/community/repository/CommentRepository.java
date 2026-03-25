package com.communityplatform.community.repository;

import com.communityplatform.community.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the Comment entity.
 *
 * ── Custom methods ───────────────────────────────────────────
 *
 *  findByPostIdAndIsDeletedFalse(postId, pageable)
 *    → SELECT * FROM comments
 *      WHERE post_id = ?
 *      AND is_deleted = false
 *      ORDER BY created_at ASC  ← set by Pageable sort
 *
 *    This is the primary query for loading the comment section
 *    of a post. It:
 *      - Filters to a single post via post_id
 *      - Excludes soft-deleted comments
 *      - Supports pagination (pass PageRequest.of(0, 20))
 *      - Supports sorting (pass Sort.by("createdAt").ascending()
 *        inside PageRequest to show oldest comments first)
 *
 *  countByPostIdAndIsDeletedFalse(postId)
 *    → SELECT COUNT(*) FROM comments
 *      WHERE post_id = ?
 *      AND is_deleted = false
 *
 *    Returns the live comment count for a post. This is used by
 *    PostService when building a PostResponse DTO so the comment
 *    count can be included in the post listing without fetching
 *    all the comment objects.
 *
 * ── Interview talking point ──────────────────────────────────
 *  Comments in this design are flat (no parent_comment_id), so
 *  findByPostId is a simple single-level fetch — one query, one
 *  result set, no recursion needed.
 *
 *  If threading were added later (nested replies), you would need
 *  either:
 *    a) A recursive CTE query (native SQL, not JPQL)
 *    b) An adjacency list + multiple queries
 *    c) A materialized path column storing the full ancestor chain
 *  All three are significantly more complex — this is exactly why
 *  we kept comments flat for this project.
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Load the comment section for a post — paginated, excludes deleted
    Page<Comment> findByPostIdAndIsDeletedFalse(Long postId, Pageable pageable);

    // Count live comments for a post — used when building PostResponse DTO
    long countByPostIdAndIsDeletedFalse(Long postId);
}

package com.communityplatform.community.repository;

import com.communityplatform.community.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for the Post entity.
 *
 * This is the most important repository in the project — it contains
 * two non-trivial custom queries worth understanding in depth.
 *
 * ── Method 1: findByCommunityIdAndIsDeletedFalse ─────────────
 *  Pure derived query method — Spring Data generates the SQL.
 *
 *  Generated SQL equivalent:
 *    SELECT * FROM posts
 *    WHERE community_id = ?
 *    AND is_deleted = false
 *    ORDER BY ... LIMIT ... OFFSET ...  ← added by Pageable
 *
 *  Returns a Page<Post> rather than List<Post>.
 *  Page wraps the results and includes total count, total pages,
 *  current page number, and whether there is a next/previous page.
 *  This enables pagination on the frontend (page=0&size=10).
 *
 * ── Method 2: findTrendingPosts (JPQL) ───────────────────────
 *  Uses a custom @Query written in JPQL (Java Persistence Query
 *  Language). JPQL looks like SQL but refers to ENTITY class names
 *  and FIELD names, not table and column names.
 *
 *  Breaking it down piece by piece:
 *
 *    SELECT p
 *      → return the full Post object (not individual columns)
 *
 *    FROM Post p
 *      → from the Post entity (maps to the `posts` table)
 *
 *    WHERE p.isDeleted = false
 *      → only include non-deleted posts
 *
 *    ORDER BY (p.upvotes + (SELECT COUNT(c) FROM Comment c
 *                           WHERE c.post = p
 *                           AND c.isDeleted = false)) DESC
 *      → trending score = upvotes + live comment count
 *      → the subquery counts only non-deleted comments for this post
 *      → DESC = highest score first
 *
 *  The Pageable parameter lets callers pass in:
 *    PageRequest.of(0, 10)  → first page, 10 results
 *    PageRequest.of(1, 10)  → second page, 10 results
 *
 * ── Method 3: findByUserIdAndIsDeletedFalse ──────────────────
 *  Derived query — finds all non-deleted posts by a specific user.
 *  Used to build a user's post history page.
 *
 * ── Interview talking point ──────────────────────────────────
 *  Q: "Why use a subquery instead of joining?"
 *  A: The subquery COUNT(c) runs once per post during the ORDER BY
 *     calculation. A JOIN would produce one row per comment (fan-out),
 *     requiring a GROUP BY to collapse them back — more complex and
 *     potentially slower on large datasets. The subquery is cleaner
 *     here because we only need the count, not the comment data.
 *
 *  Q: "Why store upvotes as a column rather than counting votes?"
 *  A: Reading is far more frequent than writing. Storing the count
 *     as a denormalized integer means the trending query never needs
 *     to touch the votes table at all — just posts + comments.
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // ── Fetch posts by community (paginated, excludes deleted) ──
    Page<Post> findByCommunityIdAndIsDeletedFalse(Long communityId, Pageable pageable);


    long countByCommunityIdAndIsDeletedFalse(Long communityId);

    // ── Trending posts: ordered by (upvotes + comment count) ───
    @Query("""
            SELECT p FROM Post p
            WHERE p.isDeleted = false
            ORDER BY (
                p.upvotes + (
                    SELECT COUNT(c) FROM Comment c
                    WHERE c.post = p
                    AND c.isDeleted = false
                )
            ) DESC
            """)
    List<Post> findTrendingPosts(Pageable pageable);

    // ── All non-deleted posts written by a specific user ────────
    Page<Post> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);
}

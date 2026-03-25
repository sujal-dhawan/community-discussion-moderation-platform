package com.communityplatform.community.entity;

import com.communityplatform.community.enums.VoteType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a single vote cast by a user on a post.
 *
 * ── Relationships ────────────────────────────────────────────
 *  Many Votes → One User  (a user can cast many votes, one per post)
 *  Many Votes → One Post  (a post can receive many votes)
 *
 * ── Interview notes ──────────────────────────────────────────
 *  - The UNIQUE constraint on (user_id, post_id) is the most
 *    important detail here. It enforces at the DATABASE level
 *    that a user can never have more than one vote row per post,
 *    regardless of what the application layer does.
 *
 *  - The application-level logic in VoteService handles three cases:
 *      1. No row exists          → INSERT new vote, increment counter
 *      2. Row exists, same type  → DELETE row (toggle off), decrement
 *      3. Row exists, diff type  → UPDATE row, swap counters
 *
 *  - Vote does NOT have `isDeleted`. When a vote is removed we
 *    physically delete the row — there is no audit need to retain it.
 *    This is different from posts/comments/reviews where soft-delete
 *    preserves content for moderation purposes.
 *
 *  - `voteType` is stored as EnumType.STRING ("UPVOTE"/"DOWNVOTE")
 *    so the column is human-readable in the database.
 */
@Entity
@Table(
    name = "votes",
    uniqueConstraints = {
        // This is the database-level guard: one vote per user per post.
        @UniqueConstraint(
            name = "uk_votes_user_post",
            columnNames = {"user_id", "post_id"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Vote direction ───────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VoteType voteType;

    // ── Audit ────────────────────────────────────────────────
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Relationships ────────────────────────────────────────
    // The user who cast this vote.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // The post being voted on.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
}

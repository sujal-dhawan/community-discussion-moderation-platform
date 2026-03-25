package com.communityplatform.community.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a flat (non-nested) comment on a post.
 *
 * ── Relationships ────────────────────────────────────────────
 *  Many Comments → One User  (each comment has one author)
 *  Many Comments → One Post  (each comment belongs to one post)
 *
 * ── Interview notes ──────────────────────────────────────────
 *  - This is deliberately flat — there is NO parent_comment_id
 *    self-referential FK. Nested (threaded) replies would require
 *    a recursive query or adjacency list pattern, adding complexity
 *    that is out of scope here.
 *  - `isDeleted` is a soft-delete flag, consistent with Post and
 *    Review. When a moderator deletes a comment, the row stays in
 *    the DB; isDeleted is set to true and the comment is filtered
 *    from all public-facing queries.
 *  - Comment does NOT have upvotes/downvotes — only posts are voted
 *    on in this design, keeping the Vote entity focused.
 */
@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Core fields ──────────────────────────────────────────
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // ── Soft delete ──────────────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // ── Audit ────────────────────────────────────────────────
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Relationships ────────────────────────────────────────
    // The comment's author.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // The post this comment belongs to.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
}

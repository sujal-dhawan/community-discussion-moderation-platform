package com.communityplatform.community.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a post created by a user inside a community.
 *
 * ── Relationships ────────────────────────────────────────────
 *  Many Posts → One User       (each post has one author)
 *  Many Posts → One Community  (each post belongs to one community)
 *  One Post   → many Comments  (a post can have many comments)
 *  One Post   → many Votes     (a post can receive many votes)
 *
 * ── Interview notes ──────────────────────────────────────────
 *  - `upvotes` and `downvotes` are DENORMALIZED counters. They are
 *    updated by VoteService whenever a vote is cast. This avoids
 *    running a COUNT(*) query on the votes table every time a post
 *    is displayed — a classic read-optimisation trade-off.
 *  - `isDeleted` is a soft-delete flag. Rather than physically removing
 *    the row (which would break foreign keys from comments and votes),
 *    we set isDeleted=true and filter it out in all queries with
 *    WHERE is_deleted = false.
 *  - The trending query uses `upvotes + COUNT(comments)` as a score,
 *    so having upvotes as a column means the sort needs only a join
 *    to comments — no subquery on the votes table needed.
 */
@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Core fields ──────────────────────────────────────────
    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // ── Denormalized vote counters ───────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private Integer upvotes = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer downvotes = 0;

    // ── Soft delete ──────────────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // ── Audit ────────────────────────────────────────────────
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Relationships (many-to-one) ──────────────────────────
    // Each post belongs to exactly one user (its author).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Each post belongs to exactly one community.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    // ── Relationships (one-to-many) ──────────────────────────
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Vote> votes = new ArrayList<>();
}

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
 * Represents a topic-based community (similar to a subreddit).
 *
 * ── Relationships ────────────────────────────────────────────
 *  Many Communities → One User  (each community has one creator)
 *  One Community    → many Posts (a community contains many posts)
 *
 * ── Interview notes ──────────────────────────────────────────
 *  - There is NO membership/join table in this simplified design.
 *    Any authenticated user can post to any community.
 *  - `createdBy` is a ManyToOne because many communities can be
 *    created by the same user.
 *  - The community name has a unique constraint — just like a
 *    subreddit name cannot be duplicated.
 *  - @JoinColumn(name = "created_by") controls the exact column
 *    name Hibernate uses in the SQL table. Without it, Hibernate
 *    would generate a default name which can be unpredictable.
 */
@Entity
@Table(name = "communities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Core fields ──────────────────────────────────────────
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;               // optional — communities don't need a description

    // ── Audit ────────────────────────────────────────────────
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Relationships ────────────────────────────────────────

    // ManyToOne — many communities can be created by one user.
    // LAZY fetch: we don't need the full User object every time
    // we load a Community — just the community data.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // OneToMany — a community contains many posts.
    // mappedBy = "community" refers to the `community` field in Post.java.
    @OneToMany(mappedBy = "community", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Post> posts = new ArrayList<>();
}

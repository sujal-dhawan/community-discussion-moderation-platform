package com.communityplatform.community.entity;

import com.communityplatform.community.enums.Role;
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
 * Represents a registered user of the platform.
 *
 * ── Relationships ────────────────────────────────────────────
 *  One User → many Posts      (a user can author multiple posts)
 *  One User → many Comments   (a user can write multiple comments)
 *  One User → many Votes      (a user can vote on multiple posts)
 *  One User → many Reviews    (a user can write multiple reviews)
 *  One User → many Reports    (a user can file multiple reports)
 *
 * ── Interview notes ──────────────────────────────────────────
 *  - Password is stored as a BCrypt hash — never plain text.
 *  - The `role` field uses @Enumerated(STRING) so the DB column
 *    contains "USER"/"MODERATOR" rather than 0/1 (ordinals are
 *    fragile if enum order ever changes).
 *  - `isActive` enables soft-banning: set to false instead of
 *    deleting the account, preserving referential integrity.
 *  - Collections are initialised to empty ArrayList to avoid
 *    NullPointerExceptions before data is loaded.
 *  - mappedBy tells JPA "the foreign key column lives on the
 *    OTHER side" (in posts.user_id, comments.user_id, etc.)
 *    so Hibernate does not create a redundant join table.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identity fields ─────────────────────────────────────
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    // ── Role & status ────────────────────────────────────────
    @Enumerated(EnumType.STRING)          // stores "USER" or "MODERATOR" in DB
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;        // every new account starts as USER

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;      // false = soft-banned

    // ── Audit ────────────────────────────────────────────────
    @CreationTimestamp                    // Hibernate sets this automatically on INSERT
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Relationships (one-to-many) ──────────────────────────
    // cascade = ALL: saving/deleting a User cascades to their posts, etc.
    // orphanRemoval = true: if a post is removed from this list, delete it from DB
    // fetch = LAZY (default for collections): do NOT load all posts every time
    //   a User is fetched — only load when explicitly accessed.

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Vote> votes = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "reporter", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Report> reports = new ArrayList<>();
}

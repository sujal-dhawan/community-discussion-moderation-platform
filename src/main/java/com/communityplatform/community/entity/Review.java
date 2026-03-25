package com.communityplatform.community.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a user-written review for a place or item.
 *
 * ── Relationships ────────────────────────────────────────────
 *  Many Reviews → One User  (each review has one author)
 *
 * ── Interview notes ──────────────────────────────────────────
 *  - Review is intentionally NOT linked to Community or Post.
 *    It is a standalone section of the platform — like a Google
 *    Maps review or a product review — separate from discussions.
 *
 *  - `subjectName` is a free-text field (e.g. "Taj Mahal Hotel",
 *    "Sony WH-1000XM5"). We don't maintain a separate `subjects`
 *    table in this simplified design, which keeps it beginner-friendly.
 *
 *  - `subjectType` is a plain string (e.g. "PLACE", "ITEM", "RESTAURANT")
 *    rather than another enum, giving flexibility without code changes.
 *
 *  - `rating` is validated as 1–5 at the DTO level with @Min(1) @Max(5).
 *    The entity itself just stores whatever integer arrives; the DTO
 *    is the right layer to enforce input rules.
 *
 *  - `isDeleted` follows the same soft-delete pattern as Post and Comment.
 */
@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── What is being reviewed ───────────────────────────────
    @Column(nullable = false, length = 200)
    private String subjectName;               // e.g. "Taj Mahal Hotel"

    @Column(nullable = false, length = 50)
    private String subjectType;               // e.g. "PLACE", "ITEM", "RESTAURANT"

    // ── Review content ───────────────────────────────────────
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer rating;                   // 1 to 5, validated in DTO layer

    // ── Soft delete ──────────────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // ── Audit ────────────────────────────────────────────────
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Relationships ────────────────────────────────────────
    // The user who wrote this review.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

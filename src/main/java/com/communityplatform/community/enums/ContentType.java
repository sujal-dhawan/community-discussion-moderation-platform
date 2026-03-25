package com.communityplatform.community.enums;

/**
 * Identifies which kind of content a Report is targeting.
 *
 * Because posts, comments, and reviews are stored in separate tables,
 * we cannot use a single foreign key to point at all three. Instead,
 * the reports table stores:
 *   - contentType  (this enum — tells us WHICH table to look in)
 *   - contentId    (the primary key within that table)
 *
 * This pattern is called a "polymorphic association".
 *
 * Example: contentType=POST, contentId=42 → go look at posts WHERE id=42
 *
 * In ModerationService we switch on this enum to route the delete
 * operation to the correct repository (PostRepository, CommentRepository,
 * or ReviewRepository).
 */
public enum ContentType {
    POST,
    COMMENT,
    REVIEW
}

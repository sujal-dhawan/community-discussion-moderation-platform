package com.communityplatform.community.enums;

/**
 * Represents the role of a registered user in the system.
 *
 * USER      — standard registered account. Can post, comment, vote, review, report.
 * MODERATOR — elevated account. Can additionally view reports and delete content.
 *
 * Guest access (unauthenticated read-only) is NOT a stored role — it is handled
 * by Spring Security permitting certain endpoints without a token.
 *
 * Stored in the `users` table as a string (EnumType.STRING) so the database
 * column contains "USER" or "MODERATOR" rather than a fragile integer ordinal.
 */
public enum Role {
    USER,
    MODERATOR
}

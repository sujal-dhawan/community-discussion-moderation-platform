package com.communityplatform.community.enums;

/**
 * Lifecycle states of a moderation report.
 *
 * PENDING   — newly filed, awaiting moderator review. This is the default
 *             value set when a report is created.
 * RESOLVED  — moderator reviewed the report and took action (e.g. deleted
 *             the reported content).
 * DISMISSED — moderator reviewed the report and found it did not violate
 *             any rules. No action was taken on the content.
 *
 * Interview talking point: using an enum here (rather than a plain String)
 * prevents invalid states from ever being stored and makes switch/if-else
 * logic in ModerationService exhaustive and compiler-checked.
 */
public enum ReportStatus {
    PENDING,
    RESOLVED,
    DISMISSED
}

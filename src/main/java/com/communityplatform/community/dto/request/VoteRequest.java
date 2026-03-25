package com.communityplatform.community.dto.request;

import com.communityplatform.community.enums.VoteType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * VoteRequest — request body for POST /api/votes/post/{postId}.
 *
 * Contains only the vote direction (UPVOTE or DOWNVOTE).
 * The postId comes from the URL path variable.
 * The voter is resolved from the JWT token.
 *
 * ── Why VoteType as enum, not a string ───────────────────────
 *  Jackson deserialises "UPVOTE" or "DOWNVOTE" directly into the
 *  VoteType enum. If the client sends anything else (e.g. "upvote",
 *  "SIDEWAYS") Jackson throws an HttpMessageNotReadableException
 *  which Spring converts to a 400 Bad Request automatically —
 *  no manual validation needed beyond @NotNull.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteRequest {

    @NotNull(message = "Vote type is required (UPVOTE or DOWNVOTE)")
    private VoteType voteType;
}

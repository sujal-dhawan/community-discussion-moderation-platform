package com.communityplatform.community.controller;

import com.communityplatform.community.dto.request.VoteRequest;
import com.communityplatform.community.dto.response.VoteResponse;
import com.communityplatform.community.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * VoteController — HTTP layer for voting.
 *
 * ── Endpoints ────────────────────────────────────────────────
 *  POST /api/votes/post/{postId}   [Authenticated]
 *
 * A single endpoint handles all three cases (new vote, toggle off,
 * flip direction) — the service determines which case applies based
 * on the existing state. This keeps the API surface minimal and
 * the client code simple: "just POST your vote intent and the
 * server figures out what to do."
 */
@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    /**
     * POST /api/votes/post/{postId}
     *
     * Casts, flips, or removes a vote on a post.
     * Returns 200 OK with updated vote counters and an action string.
     *
     * Request body:
     * { "voteType": "UPVOTE" }   or   { "voteType": "DOWNVOTE" }
     *
     * Response:
     * {
     *   "postId":    1,
     *   "voteType":  "UPVOTE",   ← null if action = "REMOVED"
     *   "action":    "VOTED",    ← "VOTED" | "CHANGED" | "REMOVED"
     *   "upvotes":   5,
     *   "downvotes": 1
     * }
     */
    @PostMapping("/post/{postId}")
    public ResponseEntity<VoteResponse> vote(
            @PathVariable Long postId,
            @Valid @RequestBody VoteRequest request
    ) {
        return ResponseEntity.ok(voteService.vote(postId, request));
    }
}

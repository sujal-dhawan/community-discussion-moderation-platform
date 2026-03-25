package com.communityplatform.community.service;

import com.communityplatform.community.dto.request.VoteRequest;
import com.communityplatform.community.dto.response.VoteResponse;
import com.communityplatform.community.entity.Post;
import com.communityplatform.community.entity.User;
import com.communityplatform.community.entity.Vote;
import com.communityplatform.community.enums.VoteType;
import com.communityplatform.community.exception.ResourceNotFoundException;
import com.communityplatform.community.repository.PostRepository;
import com.communityplatform.community.repository.VoteRepository;
import com.communityplatform.community.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * VoteService — handles the three-case toggle voting logic.
 *
 * ── The three cases ───────────────────────────────────────────
 *
 *  Case 1 — NEW VOTE
 *    No existing row for (user, post).
 *    → INSERT a Vote row, increment the relevant counter on Post.
 *    → action = "VOTED"
 *
 *  Case 2 — SAME DIRECTION (toggle off)
 *    A Vote row exists with the SAME voteType as the request.
 *    User is "un-voting" — clicking the same button again.
 *    → DELETE the Vote row, decrement the counter.
 *    → action = "REMOVED"
 *
 *  Case 3 — OPPOSITE DIRECTION (flip)
 *    A Vote row exists but with a DIFFERENT voteType.
 *    User changed their mind (upvote → downvote or vice versa).
 *    → UPDATE the Vote row's type, swap the counters.
 *    → action = "CHANGED"
 *
 * ── Why @Transactional is critical here ─────────────────────
 *  We modify TWO things in one operation: the Vote row AND the
 *  Post counters. If we update the Vote but the Post save fails
 *  (or vice versa), the data becomes inconsistent. @Transactional
 *  guarantees both writes succeed or both are rolled back.
 *
 * ── Why we update counters on the Post entity ─────────────────
 *  The Post entity stores denormalized upvotes/downvotes integers.
 *  We increment/decrement them here rather than running a COUNT
 *  on the votes table every time a post is displayed. This is a
 *  deliberate read-optimisation: reads are far more frequent than
 *  writes, so we pay a slightly more complex write for a cheaper read.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VoteService {

    private final VoteRepository voteRepository;
    private final PostRepository postRepository;
    private final SecurityUtils  securityUtils;

    @Transactional
    public VoteResponse vote(Long postId, VoteRequest request) {

        // ── Validate the post exists and is not deleted ───────────
        Post post = postRepository.findById(postId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Post not found with id: " + postId
                ));

        User currentUser = securityUtils.getCurrentUser();
        VoteType requestedType = request.getVoteType();

        // ── Check for an existing vote by this user on this post ──
        Optional<Vote> existingVote = voteRepository.findByUserIdAndPostId(
                currentUser.getId(), postId
        );

        String action;

        if (existingVote.isEmpty()) {
            // ── Case 1: New vote ──────────────────────────────────
            Vote newVote = Vote.builder()
                    .user(currentUser)
                    .post(post)
                    .voteType(requestedType)
                    .build();
            voteRepository.save(newVote);
            adjustCounter(post, requestedType, +1);
            action = "VOTED";
            log.info("New vote: postId={}, userId={}, type={}", postId, currentUser.getId(), requestedType);

        } else {
            Vote existing = existingVote.get();

            if (existing.getVoteType() == requestedType) {
                // ── Case 2: Same direction — toggle off ───────────
                voteRepository.delete(existing);
                adjustCounter(post, requestedType, -1);
                action = "REMOVED";
                log.info("Vote removed: postId={}, userId={}", postId, currentUser.getId());

            } else {
                // ── Case 3: Opposite direction — flip ────────────
                // Decrement the OLD type's counter, increment the NEW type's counter
                adjustCounter(post, existing.getVoteType(), -1);
                adjustCounter(post, requestedType, +1);
                existing.setVoteType(requestedType);
                voteRepository.save(existing);
                action = "CHANGED";
                log.info("Vote changed: postId={}, userId={}, from={} to={}",
                        postId, currentUser.getId(), existing.getVoteType(), requestedType);
            }
        }

        // Save the updated post counters
        postRepository.save(post);

        // Return the updated state
        return VoteResponse.builder()
                .postId(postId)
                .voteType(action.equals("REMOVED") ? null : requestedType)
                .action(action)
                .upvotes(post.getUpvotes())
                .downvotes(post.getDownvotes())
                .build();
    }

    /**
     * Adjusts the upvotes or downvotes counter on the Post entity by `delta`
     * (+1 to add, -1 to remove).
     *
     * Uses Math.max(0, ...) as a safety floor — the counter should never
     * go below zero even if a race condition or data issue occurs.
     */
    private void adjustCounter(Post post, VoteType type, int delta) {
        if (type == VoteType.UPVOTE) {
            post.setUpvotes(Math.max(0, post.getUpvotes() + delta));
        } else {
            post.setDownvotes(Math.max(0, post.getDownvotes() + delta));
        }
    }
}

package com.communityplatform.community.enums;

/**
 * The two possible directions a user can vote on a post.
 *
 * UPVOTE   — user likes the post. Increments posts.upvotes counter.
 * DOWNVOTE — user dislikes the post. Increments posts.downvotes counter.
 *
 * A user can only have ONE vote row per post (enforced by a unique
 * constraint on (user_id, post_id) in the votes table). Voting again
 * in the same direction removes the vote; voting in the opposite
 * direction flips it — all handled in VoteService.
 */
public enum VoteType {
    UPVOTE,
    DOWNVOTE
}

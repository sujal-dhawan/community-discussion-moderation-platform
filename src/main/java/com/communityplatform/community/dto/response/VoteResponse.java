package com.communityplatform.community.dto.response;

import com.communityplatform.community.enums.VoteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * VoteResponse — returned after a successful vote operation.
 *
 * Includes the post's updated vote counters so the client can
 * immediately update the UI without a second GET request.
 *
 * action describes what actually happened:
 *   "VOTED"   — new vote was cast
 *   "CHANGED" — vote was flipped (e.g. UPVOTE → DOWNVOTE)
 *   "REMOVED" — user voted the same way again, toggling it off
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteResponse {

    private Long     postId;
    private VoteType voteType;   // null if action = "REMOVED"
    private String   action;     // "VOTED", "CHANGED", or "REMOVED"
    private int      upvotes;    // updated post upvote count
    private int      downvotes;  // updated post downvote count
}

package com.communityplatform.community.repository;

import com.communityplatform.community.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for the Vote entity.
 *
 * ── Custom methods ───────────────────────────────────────────
 *
 *  findByUserIdAndPostId(userId, postId)
 *    → SELECT * FROM votes WHERE user_id = ? AND post_id = ?
 *
 *    This is the most-called query in the voting workflow.
 *    VoteService calls it first on every vote attempt to decide
 *    which of the three cases applies:
 *
 *      Optional.empty()  → no vote exists yet   → INSERT
 *      value present, same VoteType  → toggle off → DELETE
 *      value present, diff VoteType  → flip vote  → UPDATE + swap counters
 *
 *    Returns Optional<Vote> — the correct return type when a result
 *    may or may not exist. Never returns null.
 *
 *  existsByUserIdAndPostId(userId, postId)
 *    → SELECT 1 FROM votes WHERE user_id = ? AND post_id = ? LIMIT 1
 *
 *    A fast boolean check — used when we only need to know IF a
 *    vote exists (e.g. to mark a post as "voted" in the response
 *    DTO) without loading the Vote object itself.
 *
 * ── Interview talking point ──────────────────────────────────
 *  The votes table has a UNIQUE constraint on (user_id, post_id)
 *  defined in the Vote entity. This means the database GUARANTEES
 *  at most one row will ever match this query — findBy... will
 *  either return 0 or 1 result, never more.
 *
 *  Using Optional<Vote> instead of Vote communicates this
 *  uncertainty clearly in the method signature. The caller MUST
 *  handle the empty case — it cannot accidentally call .getVoteType()
 *  on a null and get a NullPointerException.
 */
@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    // The core lookup: does this user have a vote on this post, and if so, which way?
    Optional<Vote> findByUserIdAndPostId(Long userId, Long postId);

    // Fast check without loading the Vote object — used for response DTO enrichment
    boolean existsByUserIdAndPostId(Long userId, Long postId);
}

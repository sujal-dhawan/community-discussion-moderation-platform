package com.communityplatform.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PostResponse — the shape returned by all post-related endpoints.
 *
 * ── Fields and why each is included ─────────────────────────
 *
 *  id                — needed by the client to reference this post
 *                      in subsequent requests (e.g. GET /posts/{id},
 *                      DELETE /posts/{id}, POST /comments with postId)
 *
 *  title, content    — the post's text data
 *
 *  authorUsername    — human-readable author name. We do NOT expose
 *                      the raw userId FK — the client can display
 *                      "Posted by alice" without a second lookup.
 *
 *  communityId       — lets the client know which community this post
 *  communityName       belongs to, again without a second request.
 *
 *  upvotes           — current upvote count (denormalized on entity)
 *  downvotes         — current downvote count
 *  commentCount      — computed live from CommentRepository.
 *                      Not stored on the entity; calculated per request.
 *
 *  trendingScore     — upvotes + commentCount, pre-computed in the
 *                      service so the client can display a score.
 *                      Useful for debugging the trending order too.
 *
 *  createdAt         — post timestamp
 *
 * ── What is NOT exposed ──────────────────────────────────────
 *  - isDeleted       — internal flag, never sent to the client
 *                      (deleted posts are simply absent from listings)
 *  - user entity     — we only expose authorUsername, not the full object
 *  - votes list      — votes are on a separate endpoint
 *  - comments list   — comments are on a separate paginated endpoint
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {

    private Long   id;
    private String title;
    private String content;

    // Author info — flattened from the User relationship
    private Long   authorId;
    private String authorUsername;

    // Community info — flattened from the Community relationship
    private Long   communityId;
    private String communityName;

    // Vote counters — read from denormalized Post fields
    private Integer upvotes;
    private Integer downvotes;

    // Computed at response-build time — not stored on entity
    private long commentCount;
    private long trendingScore;   // upvotes + commentCount

    private LocalDateTime createdAt;
}

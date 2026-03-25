package com.communityplatform.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CommunityResponse — the shape returned by all community endpoints.
 *
 * ── What we include and why ──────────────────────────────────
 *  id, name, description  — the core community data
 *  createdByUsername      — human-readable creator name, NOT the raw user id.
 *                           Returning a foreign key id forces the client to
 *                           make a second request just to display a name.
 *                           Embedding the username here avoids that round-trip.
 *  postCount              — the number of posts in this community, computed
 *                           in CommunityService. Useful for listing pages
 *                           ("r/java — 142 posts") without the client needing
 *                           to call a separate endpoint.
 *  createdAt              — when the community was created.
 *
 * ── What we deliberately exclude ────────────────────────────
 *  - createdBy User entity (too much data — just expose the username)
 *  - the full List<Post> (never expose entire collections in a response —
 *    posts have their own paginated endpoint)
 *  - passwordHash, isActive, or any internal User fields
 *
 * ── @Builder pattern in CommunityService ────────────────────
 *  CommunityResponse.builder()
 *      .id(community.getId())
 *      .name(community.getName())
 *      ...
 *      .build();
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityResponse {

    private Long   id;
    private String name;
    private String description;
    private String createdByUsername;  // display name of the creator
    private long   postCount;          // computed — how many posts live here
    private LocalDateTime createdAt;
}

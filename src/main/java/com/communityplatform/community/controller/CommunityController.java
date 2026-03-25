package com.communityplatform.community.controller;

import com.communityplatform.community.dto.request.CreateCommunityRequest;
import com.communityplatform.community.dto.response.CommunityResponse;
import com.communityplatform.community.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CommunityController — HTTP layer for all community operations.
 *
 * ── Endpoint summary ─────────────────────────────────────────
 *
 *  POST   /api/communities          → createCommunity  [MODERATOR only]
 *  GET    /api/communities          → getAllCommunities [Public]
 *  GET    /api/communities/{id}     → getCommunityById [Public]
 *  GET    /api/communities/name/{name} → getCommunityByName [Public]
 *
 * ── Role enforcement — two layers ────────────────────────────
 *  Layer 1 — SecurityConfig (URL-level):
 *    .requestMatchers(HttpMethod.GET, "/api/communities/**").permitAll()
 *    .anyRequest().authenticated()
 *    → Any non-GET to /api/communities/** requires a valid token.
 *
 *  Layer 2 — @PreAuthorize (method-level):
 *    @PreAuthorize("hasRole('MODERATOR')")
 *    → Even with a valid USER token, POST is rejected with 403.
 *
 *  Using both layers is deliberate:
 *    - URL rules are broad and fast (evaluated by the filter chain)
 *    - @PreAuthorize is precise (evaluated per method, can use SpEL expressions)
 *    - Having both means a misconfigured URL rule won't accidentally
 *      expose a method that @PreAuthorize would have protected.
 *
 * ── Controller design principles ─────────────────────────────
 *  - No business logic here — delegate everything to CommunityService
 *  - No direct repository access — always go through the service
 *  - Always return ResponseEntity<T> for explicit HTTP status control
 *  - @Valid on request bodies activates Bean Validation before the
 *    method body runs
 */
@RestController
@RequestMapping("/api/communities")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    /**
     * POST /api/communities
     * Creates a new community.
     *
     * Access: MODERATOR only
     * Returns: 201 Created with the new community's full details
     *
     * Request body:
     * {
     *   "name": "java",
     *   "description": "A place to discuss Java programming"
     * }
     */
    @PostMapping
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<CommunityResponse> createCommunity(
            @Valid @RequestBody CreateCommunityRequest request
    ) {
        CommunityResponse response = communityService.createCommunity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/communities
     * Lists all communities.
     *
     * Access: Public (no token required)
     * Returns: 200 OK with a list of all communities including post counts
     */
    @GetMapping
    public ResponseEntity<List<CommunityResponse>> getAllCommunities() {
        return ResponseEntity.ok(communityService.getAllCommunities());
    }

    /**
     * GET /api/communities/{id}
     * Fetches a single community by its numeric database id.
     *
     * Access: Public
     * Returns: 200 OK with community details, or 404 if not found
     *
     * Example: GET /api/communities/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommunityResponse> getCommunityById(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.getCommunityById(id));
    }

    /**
     * GET /api/communities/name/{name}
     * Fetches a community by its unique name (like a slug).
     *
     * Access: Public
     * Returns: 200 OK with community details, or 404 if not found
     *
     * Example: GET /api/communities/name/java
     *
     * This endpoint is useful for link-style navigation where the URL
     * contains the community name rather than a numeric id —
     * similar to reddit.com/r/java.
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<CommunityResponse> getCommunityByName(@PathVariable String name) {
        return ResponseEntity.ok(communityService.getCommunityByName(name));
    }
}

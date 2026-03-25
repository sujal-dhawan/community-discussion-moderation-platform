package com.communityplatform.community.controller;

import com.communityplatform.community.dto.request.CreatePostRequest;
import com.communityplatform.community.dto.response.PostResponse;
import com.communityplatform.community.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PostController — HTTP layer for all post operations.
 *
 * ── Endpoint summary ─────────────────────────────────────────
 *
 *  POST   /api/posts                           → create a post          [Authenticated]
 *  GET    /api/posts/community/{communityId}   → list posts, paginated  [Public]
 *  GET    /api/posts/trending                  → top trending posts     [Public]
 *  DELETE /api/posts/{id}                      → soft-delete a post     [Owner or Moderator]
 *
 * ── Access control ────────────────────────────────────────────
 *  Public GET endpoints are permitted in SecurityConfig:
 *    .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
 *
 *  POST requires authentication (.anyRequest().authenticated()).
 *
 *  DELETE requires authentication too, but the owner-vs-moderator
 *  check lives inside PostService — it's business logic, not a
 *  simple role check, so @PreAuthorize alone can't express it cleanly.
 *
 * ── Pagination parameters ─────────────────────────────────────
 *  GET /api/posts/community/{id}?page=0&size=10
 *
 *  @RequestParam(defaultValue = "0")  page  → which page (0-indexed)
 *  @RequestParam(defaultValue = "10") size  → how many per page
 *
 *  Spring passes these to PostService which builds a Pageable from them.
 *  The response body includes the content list AND pagination metadata
 *  (totalElements, totalPages, last, etc.) automatically via Page<T>.
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * POST /api/posts
     *
     * Creates a new post inside a community.
     * The author is the currently authenticated user (resolved in service
     * via SecurityUtils — not passed in the request body).
     *
     * Access:  Any authenticated user (valid JWT required)
     * Returns: 201 Created + PostResponse
     *
     * Request body:
     * {
     *   "communityId": 1,
     *   "title": "Why I love Spring Boot",
     *   "content": "Here are the top reasons I recommend Spring Boot..."
     * }
     */
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request
    ) {
        PostResponse response = postService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/posts/community/{communityId}?page=0&size=10
     *
     * Returns a paginated list of non-deleted posts in a community,
     * sorted by most recently created first.
     *
     * Access:  Public — no token required
     * Returns: 200 OK + Page<PostResponse>
     *
     * The Page wrapper in the response body contains:
     *   content        → array of PostResponse objects
     *   totalElements  → total non-deleted posts in this community
     *   totalPages     → total pages at the current size
     *   number         → current page index (0-based)
     *   last           → true if this is the final page
     *
     * Example: GET /api/posts/community/1?page=0&size=5
     */
    @GetMapping("/community/{communityId}")
    public ResponseEntity<Page<PostResponse>> getPostsByCommunity(
            @PathVariable Long communityId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostResponse> posts = postService.getPostsByCommunity(communityId, page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * GET /api/posts/trending?limit=20
     *
     * Returns a flat list of top trending posts across all communities,
     * ranked by (upvotes + commentCount) descending.
     *
     * Access:  Public — no token required
     * Returns: 200 OK + List<PostResponse>
     *
     * `limit` defaults to 20 if not provided.
     * Example: GET /api/posts/trending?limit=5
     */
    @GetMapping("/trending")
    public ResponseEntity<List<PostResponse>> getTrendingPosts(
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<PostResponse> posts = postService.getTrendingPosts(limit);
        return ResponseEntity.ok(posts);
    }

    /**
     * DELETE /api/posts/{id}
     *
     * Soft-deletes a post (sets isDeleted = true).
     * Only the post's author or a MODERATOR may call this.
     * Any other authenticated user receives 403 Forbidden.
     *
     * Access:  Authenticated — owner or MODERATOR
     * Returns: 204 No Content on success
     *          404 if the post doesn't exist or is already deleted
     *          403 if the caller is neither the owner nor a moderator
     *
     * 204 No Content is the correct HTTP status for a successful DELETE
     * that returns no body — preferred over 200 OK with an empty body.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();   // 204
    }
}

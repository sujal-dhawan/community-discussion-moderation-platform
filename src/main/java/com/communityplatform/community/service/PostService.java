package com.communityplatform.community.service;

import com.communityplatform.community.dto.request.CreatePostRequest;
import com.communityplatform.community.dto.response.PostResponse;
import com.communityplatform.community.entity.Community;
import com.communityplatform.community.entity.Post;
import com.communityplatform.community.entity.User;
import com.communityplatform.community.enums.Role;
import com.communityplatform.community.exception.ResourceNotFoundException;
import com.communityplatform.community.exception.UnauthorizedException;
import com.communityplatform.community.repository.CommentRepository;
import com.communityplatform.community.repository.CommunityRepository;
import com.communityplatform.community.repository.PostRepository;
import com.communityplatform.community.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PostService — all business logic for the Post module.
 *
 * ── Four operations ───────────────────────────────────────────
 *  createPost         — authenticated user creates a post in a community
 *  getPostsByCommunity — paginated list of posts in one community
 *  getTrendingPosts   — posts sorted by upvotes + comment count
 *  deletePost         — soft-delete by owner or moderator
 *
 * ── Key dependencies ──────────────────────────────────────────
 *  PostRepository      — CRUD + custom queries for posts
 *  CommunityRepository — validates community exists before posting
 *  CommentRepository   — countByPostId for commentCount in response
 *  SecurityUtils       — gets the currently logged-in User entity
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository      postRepository;
    private final CommunityRepository communityRepository;
    private final CommentRepository   commentRepository;
    private final SecurityUtils       securityUtils;

    // ── CREATE ────────────────────────────────────────────────────
    /**
     * Creates a new post inside an existing community.
     *
     * Steps:
     *  1. Verify the target community exists — 404 if not
     *  2. Resolve the currently logged-in user as author
     *  3. Build Post entity with upvotes/downvotes defaulting to 0
     *  4. Save and return PostResponse DTO
     *
     * @Transactional — wraps the save; rolls back on any failure.
     */
    @Transactional
    public PostResponse createPost(CreatePostRequest request) {

        // ── Step 1: Community must exist ─────────────────────────
        // If the communityId doesn't resolve, fail immediately with 404
        // rather than a cryptic FK constraint violation from the DB.
        Community community = communityRepository.findById(request.getCommunityId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Community not found with id: " + request.getCommunityId()
                ));

        // ── Step 2: Get the logged-in user ────────────────────────
        User author = securityUtils.getCurrentUser();

        // ── Step 3: Build entity ──────────────────────────────────
        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .community(community)
                .user(author)
                // upvotes, downvotes, isDeleted all default via @Builder.Default on entity
                .build();

        // ── Step 4: Persist and return ────────────────────────────
        Post saved = postRepository.save(post);
        log.info("Post created: id={}, communityId={}, authorId={}",
                saved.getId(), community.getId(), author.getId());

        return toResponse(saved);
    }

    // ── READ: BY COMMUNITY (paginated) ───────────────────────────
    /**
     * Returns a paginated page of non-deleted posts for a community,
     * sorted by most recently created first.
     *
     * Callers pass page (0-indexed) and size query parameters.
     * Default: page=0, size=10 if not specified (set in controller).
     *
     * Returns Page<PostResponse> — this wrapper includes:
     *   content         → the list of PostResponse objects
     *   totalElements   → total number of non-deleted posts
     *   totalPages      → total pages at the requested size
     *   number          → current page number (0-indexed)
     *   last            → boolean: is this the last page?
     *
     * The community existence check here is important: if the caller
     * passes a non-existent communityId, we return 404 rather than
     * an empty page — an empty page would be ambiguous (does the
     * community exist but have no posts, or does it not exist at all?).
     *
     * @Transactional(readOnly = true) — no writes, skip dirty-checking.
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getPostsByCommunity(Long communityId, int page, int size) {

        // Validate the community exists first
        if (!communityRepository.existsById(communityId)) {
            throw new ResourceNotFoundException(
                    "Community not found with id: " + communityId
            );
        }

        // Build a Pageable: page number, page size, sort newest-first
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Execute the paginated query — returns only non-deleted posts
        Page<Post> postPage = postRepository.findByCommunityIdAndIsDeletedFalse(communityId, pageable);

        // Map each Post entity on the page to a PostResponse DTO
        // Page.map() preserves all pagination metadata (totalElements etc.)
        return postPage.map(this::toResponse);
    }

    // ── READ: TRENDING ────────────────────────────────────────────
    /**
     * Returns a flat list of the top N trending posts platform-wide.
     *
     * Trending score = upvotes + live comment count.
     * The JPQL query in PostRepository handles the sorting; this method
     * just controls how many results to return via Pageable.
     *
     * Returns List<PostResponse> (not Page) because trending is a
     * curated fixed-size list — pagination on a trending feed would
     * be unusual UX, and the JPQL query already embeds the ORDER BY.
     *
     * Default: returns the top 20 trending posts.
     */
    @Transactional(readOnly = true)
    public List<PostResponse> getTrendingPosts(int limit) {
        // PageRequest.of(0, limit) with no Sort — the JPQL already has ORDER BY
        Pageable pageable = PageRequest.of(0, limit);
        return postRepository.findTrendingPosts(pageable)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── DELETE (soft) ─────────────────────────────────────────────
    /**
     * Soft-deletes a post by setting isDeleted = true.
     *
     * Authorization rules:
     *   - The post's owner (author) can delete their own post
     *   - A MODERATOR can delete any post (moderation power)
     *   - Any other user attempting deletion gets a 403
     *
     * We do NOT physically remove the row. Soft delete preserves:
     *   - The comment and vote rows that reference this post (no FK violations)
     *   - A historical record for moderation audit purposes
     *   - The ability to "undelete" in the future if needed
     *
     * After soft-delete, the post disappears from all public queries
     * because every listing query filters WHERE is_deleted = false.
     *
     * @Transactional — the save must complete atomically.
     */
    @Transactional
    public void deletePost(Long postId) {

        // ── Fetch the post (must exist and not already be deleted) ─
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Post not found with id: " + postId
                ));

        if (post.getIsDeleted()) {
            throw new ResourceNotFoundException("Post not found with id: " + postId);
        }

        // ── Authorisation check ───────────────────────────────────
        User currentUser = securityUtils.getCurrentUser();

        boolean isOwner     = post.getUser().getId().equals(currentUser.getId());
        boolean isModerator = currentUser.getRole() == Role.MODERATOR;

        if (!isOwner && !isModerator) {
            throw new UnauthorizedException(
                    "You do not have permission to delete this post"
            );
        }

        // ── Soft-delete ───────────────────────────────────────────
        post.setIsDeleted(true);
        postRepository.save(post);

        log.info("Post soft-deleted: id={}, deletedByUserId={}, role={}",
                postId, currentUser.getId(), currentUser.getRole());
    }

    // ── Private helper: Entity → DTO ─────────────────────────────
    /**
     * Converts a Post entity into a PostResponse DTO.
     *
     * Called by every public method. Single place for mapping logic.
     *
     * commentCount is computed via a COUNT query — one extra DB call
     * per post. For a list of 10 posts that's 10 extra queries (N+1
     * pattern). In production you'd solve this with a JOIN or a
     * batch count query. For this project it's acceptable and honest
     * about the trade-off — a great interview discussion point.
     */
    private PostResponse toResponse(Post post) {
        long commentCount  = commentRepository.countByPostIdAndIsDeletedFalse(post.getId());
        long trendingScore = post.getUpvotes() + commentCount;

        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(post.getUser().getId())
                .authorUsername(post.getUser().getUsername())
                .communityId(post.getCommunity().getId())
                .communityName(post.getCommunity().getName())
                .upvotes(post.getUpvotes())
                .downvotes(post.getDownvotes())
                .commentCount(commentCount)
                .trendingScore(trendingScore)
                .createdAt(post.getCreatedAt())
                .build();
    }
}

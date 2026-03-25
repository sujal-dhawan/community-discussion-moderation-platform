package com.communityplatform.community.service;

import com.communityplatform.community.dto.request.CreateCommentRequest;
import com.communityplatform.community.dto.response.CommentResponse;
import com.communityplatform.community.entity.Comment;
import com.communityplatform.community.entity.Post;
import com.communityplatform.community.entity.User;
import com.communityplatform.community.enums.Role;
import com.communityplatform.community.exception.ResourceNotFoundException;
import com.communityplatform.community.exception.UnauthorizedException;
import com.communityplatform.community.repository.CommentRepository;
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

/**
 * CommentService — business logic for the Comment module.
 *
 * ── Three operations ──────────────────────────────────────────
 *  createComment    — authenticated user adds a flat comment to a post
 *  getCommentsByPost — paginated list of live comments on a post
 *  deleteComment    — soft-delete by the author or a moderator
 *
 * ── Key design decisions ──────────────────────────────────────
 *  - You cannot comment on a soft-deleted post. If a moderator removed
 *    the post, the post ID still exists in the DB but isDeleted=true.
 *    We treat that the same as "post not found" for the purpose of
 *    comment creation — there's no point adding comments to removed content.
 *  - Comments are flat (no parent_comment_id) — the entity has no
 *    self-reference, so this service stays simple.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository    postRepository;
    private final SecurityUtils     securityUtils;

    // ── CREATE ────────────────────────────────────────────────────
    /**
     * Adds a comment to an existing, non-deleted post.
     *
     * Steps:
     *  1. Load the target post — 404 if not found or already deleted
     *  2. Resolve the logged-in user as the author
     *  3. Build, save, and return the CommentResponse
     */
    @Transactional
    public CommentResponse createComment(CreateCommentRequest request) {

        // Step 1: post must exist and not be deleted
        Post post = postRepository.findById(request.getPostId())
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Post not found with id: " + request.getPostId()
                ));

        // Step 2: the author is whoever is logged in
        User author = securityUtils.getCurrentUser();

        // Step 3: build entity, save, and map to DTO
        Comment comment = Comment.builder()
                .content(request.getContent())
                .post(post)
                .user(author)
                .build();

        Comment saved = commentRepository.save(comment);
        log.info("Comment created: id={}, postId={}, authorId={}",
                saved.getId(), post.getId(), author.getId());

        return toResponse(saved);
    }

    // ── READ: BY POST (paginated) ────────────────────────────────
    /**
     * Returns a paginated page of non-deleted comments for a post,
     * sorted oldest-first (chronological thread order).
     *
     * Also validates the post exists first so callers get a clear 404
     * rather than an empty page for an unknown postId.
     *
     * @Transactional(readOnly = true) — read-only, skip dirty-checking.
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByPost(Long postId, int page, int size) {

        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post not found with id: " + postId);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());

        return commentRepository
                .findByPostIdAndIsDeletedFalse(postId, pageable)
                .map(this::toResponse);
    }

    // ── DELETE (soft) ─────────────────────────────────────────────
    /**
     * Soft-deletes a comment (isDeleted = true).
     * Only the comment author or a MODERATOR may do this.
     * Returns void — controller sends 204 No Content.
     */
    @Transactional
    public void deleteComment(Long commentId) {

        Comment comment = commentRepository.findById(commentId)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comment not found with id: " + commentId
                ));

        User currentUser = securityUtils.getCurrentUser();
        boolean isOwner     = comment.getUser().getId().equals(currentUser.getId());
        boolean isModerator = currentUser.getRole() == Role.MODERATOR;

        if (!isOwner && !isModerator) {
            throw new UnauthorizedException("You do not have permission to delete this comment");
        }

        comment.setIsDeleted(true);
        commentRepository.save(comment);

        log.info("Comment soft-deleted: id={}, by userId={}",
                commentId, currentUser.getId());
    }

    // ── Entity → DTO ─────────────────────────────────────────────
    private CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(comment.getUser().getId())
                .authorUsername(comment.getUser().getUsername())
                .postId(comment.getPost().getId())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}

package com.communityplatform.community.controller;

import com.communityplatform.community.dto.request.CreateCommentRequest;
import com.communityplatform.community.dto.response.CommentResponse;
import com.communityplatform.community.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * CommentController — HTTP layer for comment operations.
 *
 * ── Endpoints ────────────────────────────────────────────────
 *  POST   /api/comments                          [Authenticated]
 *  GET    /api/comments/post/{postId}?page=0&size=20 [Public]
 *  DELETE /api/comments/{id}                     [Owner or Moderator]
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * POST /api/comments
     * Adds a comment to a post.
     * Returns 201 Created with the saved CommentResponse.
     */
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(request));
    }

    /**
     * GET /api/comments/post/{postId}?page=0&size=20
     * Returns paginated comments for a post, oldest first.
     * Public — no token required.
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<Page<CommentResponse>> getCommentsByPost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId, page, size));
    }

    /**
     * DELETE /api/comments/{id}
     * Soft-deletes a comment. Only the author or a MODERATOR may call this.
     * Returns 204 No Content on success.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}

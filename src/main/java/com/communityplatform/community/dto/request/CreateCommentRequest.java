package com.communityplatform.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateCommentRequest — request body for POST /api/comments.
 *
 * The client tells us which post to comment on (postId)
 * and what the comment says (content).
 * The author is resolved from the JWT token in the service layer —
 * clients never send their own userId.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    @NotNull(message = "Post ID is required")
    private Long postId;

    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 2000, message = "Comment must be between 1 and 2000 characters")
    private String content;
}

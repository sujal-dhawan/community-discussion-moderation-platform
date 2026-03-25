package com.communityplatform.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CommentResponse — returned for every comment-related endpoint.
 *
 * Flat structure: author and post identity are flattened into simple
 * fields rather than nested objects, keeping the JSON clean and
 * avoiding over-fetching on the client side.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {

    private Long   id;
    private String content;

    // Author info — flattened from the User relationship
    private Long   authorId;
    private String authorUsername;

    // Post info — client needs postId to navigate back to the post
    private Long   postId;

    private LocalDateTime createdAt;
}

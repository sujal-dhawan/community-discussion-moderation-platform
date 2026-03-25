package com.communityplatform.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ReviewResponse — returned for all review-related endpoints.
 *
 * Includes the author's username (flattened from the User relationship)
 * so the client can display "Reviewed by alice" without a second request.
 * The authorId is also included for client-side ownership checks
 * (e.g. to decide whether to show a Delete button).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private Long    id;
    private String  subjectName;
    private String  subjectType;
    private String  content;
    private Integer rating;

    // Author info — flattened
    private Long   authorId;
    private String authorUsername;

    private LocalDateTime createdAt;
}

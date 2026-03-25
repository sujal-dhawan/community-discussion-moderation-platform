package com.communityplatform.community.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource (user, post, community, etc.)
 * cannot be found in the database.
 *
 * Maps to HTTP 404 Not Found.
 * Used throughout the service layer whenever a findById() or
 * findByXxx() returns an empty Optional.
 *
 * Example usage in a service:
 *   Post post = postRepository.findById(id)
 *       .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

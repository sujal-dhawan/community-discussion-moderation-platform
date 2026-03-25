package com.communityplatform.community.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a registration attempt uses an email or username
 * that already exists in the database.
 *
 * @ResponseStatus(HttpStatus.CONFLICT) means if this exception
 * escapes a controller method WITHOUT being caught by the global
 * exception handler, Spring will automatically return HTTP 409.
 * In practice, GlobalExceptionHandler catches it first and shapes
 * the response body — but the annotation documents intent clearly.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}

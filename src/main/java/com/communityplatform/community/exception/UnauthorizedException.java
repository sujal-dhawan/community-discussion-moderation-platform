package com.communityplatform.community.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user attempts an action they are not permitted to perform.
 *
 * Examples:
 *   - A USER tries to delete another user's post
 *   - A USER tries to access a MODERATOR-only endpoint
 *   - Someone tries to vote twice on the same post
 *
 * Maps to HTTP 403 Forbidden.
 *
 * Note: HTTP 401 Unauthorized means "not authenticated" (no token).
 *       HTTP 403 Forbidden means "authenticated but not allowed".
 *       This exception is for the 403 case — the user is logged in
 *       but does not have permission for this specific action.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}

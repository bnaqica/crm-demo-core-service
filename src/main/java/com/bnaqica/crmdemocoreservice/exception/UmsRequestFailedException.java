package com.bnaqica.crmdemocoreservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * A UMS call made on a user's behalf failed. The UMS's 409 and 429 are passed on as they are, since the caller can act
 * on them; anything else, including an unreachable UMS, is a 502.
 */
@Getter
public class UmsRequestFailedException extends RuntimeException {

    private final HttpStatus responseStatus;

    public UmsRequestFailedException(String message, UmsClientException cause) {
        super(message, cause);
        this.responseStatus = switch (cause.getStatus()) {
            case 409 -> HttpStatus.CONFLICT;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }

    public UmsRequestFailedException(String message, Throwable cause) {
        super(message, cause);
        this.responseStatus = HttpStatus.BAD_GATEWAY;
    }
}

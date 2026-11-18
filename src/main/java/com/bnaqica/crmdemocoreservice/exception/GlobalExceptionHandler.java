package com.bnaqica.crmdemocoreservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Turns exceptions into {@link ProblemDetail} responses. Add a handler here for each custom exception. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidStatusTransitionException.class)
    ProblemDetail handleStatusTransition(InvalidStatusTransitionException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(VersionMismatchException.class)
    ProblemDetail handleVersionMismatch(VersionMismatchException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    /** Someone else changed the record between our read and our write. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentChange(OptimisticLockingFailureException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "The record was changed at the same time; reload it and try again");
    }

    @ExceptionHandler(UmsRequestFailedException.class)
    ProblemDetail handleUmsRequestFailed(UmsRequestFailedException exception) {
        if (exception.getResponseStatus() == HttpStatus.BAD_GATEWAY) {
            log.error("UMS call failed", exception);
            return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "The User Management Service call failed");
        }
        String detail = exception.getResponseStatus() == HttpStatus.CONFLICT
                ? "The portal user can't be promoted in their current state"
                : "Too many requests to the User Management Service; try again later";
        return ProblemDetail.forStatusAndDetail(exception.getResponseStatus(), detail);
    }

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail handleInvalidRequest(InvalidRequestException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
    }

    @ExceptionHandler(RecordNotFoundException.class)
    ProblemDetail handleNotFound(RecordNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /** Access can't be decided without the UMS; writes fail closed. */
    @ExceptionHandler(UmsUnavailableException.class)
    ProblemDetail handleUmsUnavailable(UmsUnavailableException exception) {
        log.error("UMS unavailable", exception);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "Access can't be checked right now; try again shortly");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) throws Exception {
        // Spring Security has to see these to answer 401 or 403
        if (exception instanceof AccessDeniedException || exception instanceof AuthenticationException) {
            throw exception;
        }
        log.error("Unhandled exception", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }
}

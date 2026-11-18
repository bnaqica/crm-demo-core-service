package com.bnaqica.crmdemocoreservice.exception;

import org.springframework.security.access.AccessDeniedException;

/** The caller may not change or manage the team's records. */
public class TeamAccessDeniedException extends AccessDeniedException {

    public TeamAccessDeniedException(String message) {
        super(message);
    }
}

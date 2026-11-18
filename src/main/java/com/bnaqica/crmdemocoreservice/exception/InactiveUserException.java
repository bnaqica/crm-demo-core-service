package com.bnaqica.crmdemocoreservice.exception;

import org.springframework.security.access.AccessDeniedException;

/** The caller's token is valid, but their UMS account isn't ACTIVE (disabled, or not an internal user). */
public class InactiveUserException extends AccessDeniedException {

    public InactiveUserException(String sub, String status) {
        super("User " + sub + " is " + status);
    }
}

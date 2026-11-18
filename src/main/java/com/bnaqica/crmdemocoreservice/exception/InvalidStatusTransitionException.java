package com.bnaqica.crmdemocoreservice.exception;

/** The record's current status doesn't allow the change, for example deciding a submission twice; answered with 409. */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}

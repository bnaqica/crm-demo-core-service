package com.bnaqica.crmdemocoreservice.exception;

/** The record doesn't exist, or the caller may not see it; the two aren't told apart, so existence doesn't leak. */
public class RecordNotFoundException extends RuntimeException {

    public RecordNotFoundException(String type, Object id) {
        super(type + " " + id + " not found");
    }
}

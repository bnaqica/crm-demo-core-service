package com.bnaqica.crmdemocoreservice.exception;

/** An event that doesn't match its contract; it goes to the dead-letter topic without retries. */
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message) {
        super(message);
    }
}

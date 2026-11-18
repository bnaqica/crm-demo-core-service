package com.bnaqica.crmdemocoreservice.exception;

/** A request that is well-formed but can't be applied; answered with 422. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}

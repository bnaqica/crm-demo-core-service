package com.bnaqica.crmdemocoreservice.exception;

/** The UMS couldn't answer, so access can't be decided; answered with 503. */
public class UmsUnavailableException extends RuntimeException {

    public UmsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.bnaqica.crmdemocoreservice.exception;

/** The record changed since the user looked at it (a portal correction, for example); answered with 409. */
public class VersionMismatchException extends RuntimeException {

    public VersionMismatchException(String message) {
        super(message);
    }
}

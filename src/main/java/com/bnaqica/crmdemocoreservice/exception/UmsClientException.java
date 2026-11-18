package com.bnaqica.crmdemocoreservice.exception;

import lombok.Getter;

/** A UMS call returned an error. {@code status} is the UMS's HTTP status. */
@Getter
public class UmsClientException extends RuntimeException {

    private final int status;

    public UmsClientException(int status, String message) {
        super(message);
        this.status = status;
    }

    public boolean isNotFound() {
        return status == 404;
    }
}

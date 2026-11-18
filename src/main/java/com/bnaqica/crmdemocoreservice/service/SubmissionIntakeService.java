package com.bnaqica.crmdemocoreservice.service;

/** Handles messages from {@code portal-intake}. */
public interface SubmissionIntakeService {

    /**
     * Validates the event, skips it if its id was already handled, and applies it.
     *
     * @param type the message's {@code type} header
     * @throws com.bnaqica.crmdemocoreservice.exception.InvalidEventException if the event breaks its contract
     */
    void handle(String type, String payload);
}

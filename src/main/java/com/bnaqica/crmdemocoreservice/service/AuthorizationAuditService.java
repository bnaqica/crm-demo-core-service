package com.bnaqica.crmdemocoreservice.service;

import java.util.UUID;

/**
 * Publishes authorization audit events for the current request and user to {@code audit-events}, through the outbox.
 * Each is written in a transaction of its own, so it survives a rollback of the request's work.
 */
public interface AuthorizationAuditService {

    /** @param permission the permission the endpoint needs, or null if the denial isn't about one */
    void denied(String permission, String reason);

    void crossTeamRead(UUID teamId, String recordType, String recordId);
}

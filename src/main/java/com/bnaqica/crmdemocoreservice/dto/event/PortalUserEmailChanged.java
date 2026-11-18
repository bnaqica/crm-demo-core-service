package com.bnaqica.crmdemocoreservice.dto.event;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code PortalUserEmailChanged} v1 from the portal on {@code portal-intake}. Schema:
 * {@code schemas/portal-user-email-changed-v1.json}.
 */
public record PortalUserEmailChanged(
        UUID eventId, int schemaVersion, Instant occurredAt, UUID portalUserId, String email) {}

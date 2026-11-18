package com.bnaqica.crmdemocoreservice.dto.event;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * Published to {@code audit-events}. Follows the common audit schema shared by Keycloak, UMS, CRM and portal records,
 * plus {@code permission}, {@code path} and {@code teamId}, which authorization records need.
 *
 * @param path the request, as {@code METHOD /path}
 * @param reason why access was denied; null otherwise
 */
@Builder
public record AuditEvent(
        UUID eventId,
        int schemaVersion,
        Instant occurredAt,
        String source,
        String realm,
        String actorId,
        String actorType,
        AuditAction action,
        String targetType,
        String targetId,
        String outcome,
        String permission,
        String path,
        UUID teamId,
        String ipAddress,
        String correlationId,
        String reason) {}

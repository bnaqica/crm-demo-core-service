package com.bnaqica.crmdemocoreservice.dto.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

/**
 * {@code SubmissionReceived} v1 from the portal on {@code portal-intake} (header {@code type}), keyed by
 * {@code portalUserId}; sent for each new submission and each correction (higher {@code version}). Schema:
 * {@code schemas/submission-received-v1.json}.
 */
@Builder
public record SubmissionReceived(
        UUID eventId,
        int schemaVersion,
        Instant occurredAt,
        UUID portalUserId,
        String email,
        UUID submissionId,
        int version,
        String type,
        String region,
        List<DocumentReference> documents) {

    public record DocumentReference(UUID documentId, String storageKey, String contentType, long size, String sha256) {}
}

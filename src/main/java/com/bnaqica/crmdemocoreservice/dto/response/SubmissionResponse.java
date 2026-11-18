package com.bnaqica.crmdemocoreservice.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

/**
 * A portal submission as internal users see it. Documents are references; the files stay in the portal.
 *
 * @param version the portal's version of the submission; corrections raise it
 * @param rejectionReason shown to the submitter; only when REJECTED
 */
@Builder
public record SubmissionResponse(
        UUID id,
        int version,
        String type,
        String region,
        String status,
        UUID teamId,
        UUID portalUserId,
        String submitterEmail,
        Instant receivedAt,
        Instant decidedAt,
        String rejectionReason,
        List<Document> documents) {

    public record Document(UUID documentId, String contentType, long size, String sha256) {}
}

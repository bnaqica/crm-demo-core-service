package com.bnaqica.crmdemocoreservice.dto.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * {@code SubmissionAccepted} or {@code SubmissionRejected} v1, published to {@code submission-status} for the portal,
 * keyed by {@code portalUserId}. Carries no CRM data beyond these fields. Schema:
 * {@code schemas/submission-status-v1.json}.
 *
 * @param status {@code ACCEPTED} or {@code REJECTED}
 * @param reason shown to the submitter; only when rejected
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubmissionStatusChanged(
        UUID eventId,
        int schemaVersion,
        Instant occurredAt,
        UUID submissionId,
        int version,
        UUID portalUserId,
        String status,
        String reason) {}

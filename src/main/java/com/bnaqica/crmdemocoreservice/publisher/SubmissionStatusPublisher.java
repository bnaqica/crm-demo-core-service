package com.bnaqica.crmdemocoreservice.publisher;

import com.bnaqica.crmdemocoreservice.config.CrmProperties;
import com.bnaqica.crmdemocoreservice.dto.event.SubmissionStatusChanged;
import com.bnaqica.crmdemocoreservice.entity.Submission;
import com.bnaqica.crmdemocoreservice.service.EventSchemas;
import com.bnaqica.crmdemocoreservice.service.OutboxService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes {@code SubmissionAccepted} / {@code SubmissionRejected} to the outbox for {@code submission-status}, keyed by
 * the portal user. Call inside the transaction that decides the submission, so the event is published only if the
 * decision commits. The event is checked against its schema first, so no CRM data can slip into it.
 */
@Component
@RequiredArgsConstructor
public class SubmissionStatusPublisher {

    static final int SCHEMA_VERSION = 1;

    private final OutboxService outbox;
    private final JsonMapper jsonMapper;
    private final EventSchemas schemas;
    private final CrmProperties properties;
    private final Clock clock;

    public void decided(Submission submission) {
        UUID portalUser = submission.getPortalUser().getId();
        JsonNode event = jsonMapper.valueToTree(SubmissionStatusChanged.builder()
                .eventId(UUID.randomUUID())
                .schemaVersion(SCHEMA_VERSION)
                .occurredAt(Instant.now(clock))
                .submissionId(submission.getId())
                .version(submission.getVersion())
                .portalUserId(portalUser)
                .status(submission.getStatus().name())
                .reason(submission.getRejectionReason())
                .build());
        schemas.validate(EventSchemas.SUBMISSION_STATUS, event);
        outbox.add(properties.topics().submissionStatus(), portalUser.toString(), jsonMapper.writeValueAsString(event));
    }
}

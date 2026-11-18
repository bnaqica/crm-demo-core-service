package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.config.CrmProperties;
import com.bnaqica.crmdemocoreservice.dto.event.PortalUserEmailChanged;
import com.bnaqica.crmdemocoreservice.dto.event.SubmissionReceived;
import com.bnaqica.crmdemocoreservice.entity.PortalUser;
import com.bnaqica.crmdemocoreservice.entity.Submission;
import com.bnaqica.crmdemocoreservice.entity.SubmissionDocument;
import com.bnaqica.crmdemocoreservice.entity.SubmissionStatus;
import com.bnaqica.crmdemocoreservice.exception.InvalidEventException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Applies {@code portal-intake} events in one transaction with their {@code processed_event} row, so each event id
 * takes effect at most once.
 *
 * <ul>
 *   <li>{@code SubmissionReceived} for a new submission: routed to the first matching rule's team, or the default
 *       team, with status RECEIVED. For a known submission with a higher version (a correction from the portal): its
 *       type, region and documents are replaced, but it keeps its team and status. Older versions are ignored.
 *   <li>{@code PortalUserEmailChanged}: the stored copy of the user gets the new email.
 * </ul>
 */
@Slf4j
@Service
public class SubmissionIntakeServiceImpl implements SubmissionIntakeService {

    private final EventSchemas schemas;
    private final JsonMapper jsonMapper;
    private final ProcessedEventService processed;
    private final PortalUserService portalUsers;
    private final SubmissionService submissions;
    private final RoutingRuleService routingRules;
    private final Clock clock;
    private final UUID defaultTeam;

    @Autowired
    public SubmissionIntakeServiceImpl(
            EventSchemas schemas,
            JsonMapper jsonMapper,
            ProcessedEventService processed,
            PortalUserService portalUsers,
            SubmissionService submissions,
            RoutingRuleService routingRules,
            Clock clock,
            CrmProperties properties) {
        this(
                schemas,
                jsonMapper,
                processed,
                portalUsers,
                submissions,
                routingRules,
                clock,
                properties.routing().defaultTeamId());
    }

    SubmissionIntakeServiceImpl(
            EventSchemas schemas,
            JsonMapper jsonMapper,
            ProcessedEventService processed,
            PortalUserService portalUsers,
            SubmissionService submissions,
            RoutingRuleService routingRules,
            Clock clock,
            UUID defaultTeam) {
        this.schemas = schemas;
        this.jsonMapper = jsonMapper;
        this.processed = processed;
        this.portalUsers = portalUsers;
        this.submissions = submissions;
        this.routingRules = routingRules;
        this.clock = clock;
        this.defaultTeam = defaultTeam;
    }

    @Override
    @Transactional
    public void handle(String type, String payload) {
        JsonNode event = jsonMapper.readTree(payload);
        if (type == null) {
            throw new InvalidEventException("Missing type header");
        }
        schemas.validate(type, event);
        switch (type) {
            case EventSchemas.SUBMISSION_RECEIVED -> apply(jsonMapper.treeToValue(event, SubmissionReceived.class));
            case EventSchemas.PORTAL_USER_EMAIL_CHANGED ->
                apply(jsonMapper.treeToValue(event, PortalUserEmailChanged.class));
            default -> throw new InvalidEventException("Event type " + type + " isn't consumed from portal-intake");
        }
    }

    private void apply(SubmissionReceived event) {
        if (!processed.markProcessed(event.eventId())) {
            log.debug("Skipping already processed event {}", event.eventId());
            return;
        }
        PortalUser submitter = portalUsers.upsert(event.portalUserId(), event.email());
        Optional<Submission> existing = submissions.find(event.submissionId());
        if (existing.isEmpty()) {
            create(event, submitter);
        } else if (event.version() > existing.get().getVersion()) {
            correct(existing.get(), event);
        } else {
            log.debug(
                    "Ignoring version {} of submission {}, which is at version {}",
                    event.version(),
                    event.submissionId(),
                    existing.get().getVersion());
        }
    }

    private void create(SubmissionReceived event, PortalUser submitter) {
        UUID team = routingRules.teamFor(event.type(), event.region()).orElse(defaultTeam);
        submissions.save(Submission.builder()
                .id(event.submissionId())
                .portalUser(submitter)
                .version(event.version())
                .type(event.type())
                .region(event.region())
                .teamId(team)
                .status(SubmissionStatus.RECEIVED)
                .receivedAt(Instant.now(clock))
                .documents(documents(event))
                .build());
        log.info("Received submission {} for team {}", event.submissionId(), team);
    }

    private void correct(Submission submission, SubmissionReceived event) {
        submission.setVersion(event.version());
        submission.setType(event.type());
        submission.setRegion(event.region());
        submission.getDocuments().clear();
        submission.getDocuments().addAll(documents(event));
        submissions.save(submission);
        log.info("Updated submission {} to version {}", event.submissionId(), event.version());
    }

    private static List<SubmissionDocument> documents(SubmissionReceived event) {
        return new ArrayList<>(event.documents().stream()
                .map(document -> new SubmissionDocument(
                        document.documentId(),
                        document.storageKey(),
                        document.contentType(),
                        document.size(),
                        document.sha256()))
                .toList());
    }

    private void apply(PortalUserEmailChanged event) {
        if (!processed.markProcessed(event.eventId())) {
            log.debug("Skipping already processed event {}", event.eventId());
            return;
        }
        portalUsers.upsert(event.portalUserId(), event.email());
    }
}

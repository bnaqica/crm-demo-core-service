package com.bnaqica.crmdemocoreservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bnaqica.crmdemocoreservice.entity.PortalUser;
import com.bnaqica.crmdemocoreservice.entity.Submission;
import com.bnaqica.crmdemocoreservice.entity.SubmissionStatus;
import com.bnaqica.crmdemocoreservice.exception.InvalidEventException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class SubmissionIntakeServiceImplTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");
    private static final UUID CLAIMS_EU = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID DEFAULT_TEAM = UUID.fromString("00000000-0000-0000-0000-0000000000d0");

    private final ProcessedEventService processed = mock(ProcessedEventService.class);
    private final PortalUserService portalUsers = mock(PortalUserService.class);
    private final SubmissionService submissions = mock(SubmissionService.class);
    private final RoutingRuleService routingRules = mock(RoutingRuleService.class);
    private final SubmissionIntakeService intake = new SubmissionIntakeServiceImpl(
            new EventSchemas(JSON),
            JSON,
            processed,
            portalUsers,
            submissions,
            routingRules,
            Clock.fixed(NOW, ZoneOffset.UTC),
            DEFAULT_TEAM);

    private final UUID eventId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID submissionId = UUID.randomUUID();

    private String received(int version, String type, String region) {
        return """
                {"eventId":"%s","schemaVersion":1,"occurredAt":"2026-11-12T09:00:00Z","portalUserId":"%s",
                 "email":"ana@acme.example","submissionId":"%s","version":%d,"type":"%s","region":"%s",
                 "documents":[{"documentId":"%s","storageKey":"k","contentType":"application/pdf","size":1,
                               "sha256":"%s"}]}
                """.formatted(eventId, userId, submissionId, version, type, region, UUID.randomUUID(), "a".repeat(64));
    }

    private PortalUser user() {
        return PortalUser.builder()
                .id(userId)
                .email("ana@acme.example")
                .updatedAt(NOW)
                .build();
    }

    private Submission created() {
        var saved = ArgumentCaptor.forClass(Submission.class);
        verify(submissions).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void newSubmissionGoesToTheFirstMatchingRulesTeam() {
        when(processed.markProcessed(eventId)).thenReturn(true);
        when(portalUsers.upsert(userId, "ana@acme.example")).thenReturn(user());
        when(submissions.find(submissionId)).thenReturn(Optional.empty());
        when(routingRules.teamFor("CLAIM", "EU")).thenReturn(Optional.of(CLAIMS_EU));

        intake.handle("SubmissionReceived", received(1, "CLAIM", "EU"));

        Submission submission = created();
        assertThat(submission.getId()).isEqualTo(submissionId);
        assertThat(submission.getTeamId()).isEqualTo(CLAIMS_EU);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.RECEIVED);
        assertThat(submission.getReceivedAt()).isEqualTo(NOW);
        assertThat(submission.getDocuments()).hasSize(1);
    }

    @Test
    void submissionMatchingNoRuleGoesToTheDefaultTeam() {
        when(processed.markProcessed(eventId)).thenReturn(true);
        when(portalUsers.upsert(userId, "ana@acme.example")).thenReturn(user());
        when(submissions.find(submissionId)).thenReturn(Optional.empty());
        when(routingRules.teamFor("OTHER", "US")).thenReturn(Optional.empty());

        intake.handle("SubmissionReceived", received(1, "OTHER", "US"));

        assertThat(created().getTeamId()).isEqualTo(DEFAULT_TEAM);
    }

    @Test
    void alreadyProcessedEventChangesNothing() {
        when(processed.markProcessed(eventId)).thenReturn(false);

        intake.handle("SubmissionReceived", received(1, "CLAIM", "EU"));

        verifyNoInteractions(portalUsers, submissions, routingRules);
    }

    @Test
    void invalidEventIsRejectedBeforeAnythingIsRecorded() {
        String event = received(1, "CLAIM", "EU").replace("\"schemaVersion\":1", "\"schemaVersion\":99");

        assertThatThrownBy(() -> intake.handle("SubmissionReceived", event)).isInstanceOf(InvalidEventException.class);
        verifyNoInteractions(processed, submissions);
    }

    @Test
    void eventWithoutTypeHeaderIsRejected() {
        assertThatThrownBy(() -> intake.handle(null, received(1, "CLAIM", "EU")))
                .isInstanceOf(InvalidEventException.class);
    }

    @Test
    void correctionUpdatesTheSubmissionButKeepsItsTeamAndStatus() {
        Submission existing = Submission.builder()
                .id(submissionId)
                .portalUser(user())
                .version(1)
                .type("CLAIM")
                .region("EU")
                .teamId(CLAIMS_EU)
                .status(SubmissionStatus.RECEIVED)
                .receivedAt(NOW.minusSeconds(60))
                .documents(new java.util.ArrayList<>())
                .build();
        when(processed.markProcessed(eventId)).thenReturn(true);
        when(portalUsers.upsert(userId, "ana@acme.example")).thenReturn(user());
        when(submissions.find(submissionId)).thenReturn(Optional.of(existing));

        intake.handle("SubmissionReceived", received(2, "INVOICE", "EU"));

        Submission saved = created();
        assertThat(saved.getVersion()).isEqualTo(2);
        assertThat(saved.getType()).isEqualTo("INVOICE");
        assertThat(saved.getDocuments()).hasSize(1);
        assertThat(saved.getTeamId()).isEqualTo(CLAIMS_EU);
        assertThat(saved.getStatus()).isEqualTo(SubmissionStatus.RECEIVED);
        verify(routingRules, never()).teamFor(any(), any());
    }

    @Test
    void olderVersionOfAKnownSubmissionIsIgnored() {
        Submission existing = Submission.builder()
                .id(submissionId)
                .version(3)
                .documents(List.of())
                .build();
        when(processed.markProcessed(eventId)).thenReturn(true);
        when(portalUsers.upsert(userId, "ana@acme.example")).thenReturn(user());
        when(submissions.find(submissionId)).thenReturn(Optional.of(existing));

        intake.handle("SubmissionReceived", received(2, "CLAIM", "EU"));

        verify(submissions, never()).save(any());
    }

    @Test
    void emailChangeUpdatesTheStoredCopy() {
        when(processed.markProcessed(eventId)).thenReturn(true);

        intake.handle("PortalUserEmailChanged", """
                {"eventId":"%s","schemaVersion":1,"occurredAt":"2026-11-12T09:00:00Z","portalUserId":"%s",
                 "email":"ana.new@acme.example"}
                """.formatted(eventId, userId));

        verify(portalUsers).upsert(userId, "ana.new@acme.example");
    }
}

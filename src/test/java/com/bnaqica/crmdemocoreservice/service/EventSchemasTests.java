package com.bnaqica.crmdemocoreservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bnaqica.crmdemocoreservice.dto.event.SubmissionReceived;
import com.bnaqica.crmdemocoreservice.dto.event.SubmissionStatusChanged;
import com.bnaqica.crmdemocoreservice.exception.InvalidEventException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class EventSchemasTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final EventSchemas schemas = new EventSchemas(JSON);

    static String submissionReceived() {
        return """
                {"eventId":"%s","schemaVersion":1,"occurredAt":"2026-11-12T10:00:00Z",
                 "portalUserId":"%s","email":"ana@acme.example","submissionId":"%s","version":1,
                 "type":"CLAIM","region":"EU",
                 "documents":[{"documentId":"%s","storageKey":"u/1/invoice.pdf","contentType":"application/pdf",
                               "size":1234,"sha256":"%s"}]}
                """.formatted(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ab".repeat(32));
    }

    private static ObjectNode tree(String json) {
        return (ObjectNode) JSON.readTree(json);
    }

    @Test
    void validSubmissionReceivedPasses() {
        JsonNode event = tree(submissionReceived());

        assertThatCode(() -> schemas.validate(EventSchemas.SUBMISSION_RECEIVED, event))
                .doesNotThrowAnyException();
        assertThat(JSON.treeToValue(event, SubmissionReceived.class).documents())
                .hasSize(1);
    }

    @Test
    void unknownSchemaVersionIsRejected() {
        ObjectNode event = tree(submissionReceived()).put("schemaVersion", 99);

        assertThatThrownBy(() -> schemas.validate(EventSchemas.SUBMISSION_RECEIVED, event))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("schemaVersion 99");
    }

    @Test
    void missingEmailIsRejected() {
        ObjectNode event = tree(submissionReceived());
        event.remove("email");

        assertThatThrownBy(() -> schemas.validate(EventSchemas.SUBMISSION_RECEIVED, event))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("email");
    }

    @Test
    void malformedDocumentReferenceIsRejected() {
        ObjectNode event = tree(submissionReceived());
        ((ObjectNode) event.get("documents").get(0)).put("sha256", "not-a-hash");

        assertThatThrownBy(() -> schemas.validate(EventSchemas.SUBMISSION_RECEIVED, event))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("sha256");
    }

    @Test
    void nonUuidSubmissionIdIsRejected() {
        ObjectNode event = tree(submissionReceived()).put("submissionId", "42");

        assertThatThrownBy(() -> schemas.validate(EventSchemas.SUBMISSION_RECEIVED, event))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("submissionId");
    }

    @Test
    void unknownEventTypeIsRejected() {
        assertThatThrownBy(() -> schemas.validate("SomethingElse", tree(submissionReceived())))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("SomethingElse");
    }

    @Test
    void emailChangeNeedsAValidEmail() {
        ObjectNode event = tree("""
                {"eventId":"%s","schemaVersion":1,"occurredAt":"2026-11-12T10:00:00Z","portalUserId":"%s",
                 "email":"ana@acme.example"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID()));
        assertThatCode(() -> schemas.validate(EventSchemas.PORTAL_USER_EMAIL_CHANGED, event))
                .doesNotThrowAnyException();

        event.put("email", "not an email");
        assertThatThrownBy(() -> schemas.validate(EventSchemas.PORTAL_USER_EMAIL_CHANGED, event))
                .isInstanceOf(InvalidEventException.class);
    }

    @Test
    void statusEventWeBuildMatchesTheContract() {
        JsonNode event = JSON.valueToTree(status("REJECTED", "Missing invoice"));

        assertThatCode(() -> schemas.validate(EventSchemas.SUBMISSION_STATUS, event))
                .doesNotThrowAnyException();
    }

    @Test
    void statusEventMayCarryNoCrmData() {
        ObjectNode event = JSON.valueToTree(status("ACCEPTED", null));
        event.put("teamId", UUID.randomUUID().toString());

        assertThatThrownBy(() -> schemas.validate(EventSchemas.SUBMISSION_STATUS, event))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("teamId");
    }

    @Test
    void rejectionNeedsAReason() {
        JsonNode event = JSON.valueToTree(status("REJECTED", null));

        assertThatThrownBy(() -> schemas.validate(EventSchemas.SUBMISSION_STATUS, event))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("reason");
    }

    private static SubmissionStatusChanged status(String status, String reason) {
        return SubmissionStatusChanged.builder()
                .eventId(UUID.randomUUID())
                .schemaVersion(1)
                .occurredAt(Instant.parse("2026-11-12T10:00:00Z"))
                .submissionId(UUID.randomUUID())
                .version(1)
                .portalUserId(UUID.randomUUID())
                .status(status)
                .reason(reason)
                .build();
    }
}

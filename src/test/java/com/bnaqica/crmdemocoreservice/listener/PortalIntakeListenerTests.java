package com.bnaqica.crmdemocoreservice.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.bnaqica.crmdemocoreservice.dto.request.CreateRoutingRuleRequest;
import com.bnaqica.crmdemocoreservice.repository.SubmissionRepository;
import com.bnaqica.crmdemocoreservice.service.RoutingRuleService;
import com.bnaqica.crmdemocoreservice.service.SubmissionService;
import com.bnaqica.crmdemocoreservice.support.IntegrationTest;
import com.bnaqica.crmdemocoreservice.support.TestKafka;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.kafka.KafkaContainer;

/** The portal-intake consumer end to end: Kafka, validation, dedup, routing and the dead-letter topic. */
class PortalIntakeListenerTests extends IntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Autowired
    private SubmissionService submissions;

    @Autowired
    private RoutingRuleService routingRules;

    @Autowired
    private SubmissionRepository repository;

    @Value("${crm.routing.default-team-id}")
    private UUID defaultTeam;

    private final UUID portalUser = UUID.randomUUID();
    // Unique per test, so rules and submissions of other tests in the shared database don't interfere
    private final String region = "EU-" + UUID.randomUUID();

    static String received(UUID eventId, UUID portalUser, UUID submissionId, String type, String region) {
        return """
                {"eventId":"%s","schemaVersion":1,"occurredAt":"2026-11-12T09:00:00Z","portalUserId":"%s",
                 "email":"ana@acme.example","submissionId":"%s","version":1,"type":"%s","region":"%s",
                 "documents":[{"documentId":"%s","storageKey":"k","contentType":"application/pdf","size":1,
                               "sha256":"%s"}]}
                """.formatted(eventId, portalUser, submissionId, type, region, UUID.randomUUID(), "a".repeat(64));
    }

    private void send(String type, String payload) {
        var record = new ProducerRecord<String, String>("portal-intake", portalUser.toString(), payload);
        if (type != null) {
            record.headers().add("type", type.getBytes(StandardCharsets.UTF_8));
        }
        kafka.send(record);
    }

    @Test
    void matchingSubmissionBelongsToTheRulesTeamWithStatusReceived() {
        UUID claimsEu = UUID.randomUUID();
        routingRules.create(new CreateRoutingRuleRequest("CLAIM", region, claimsEu));
        UUID submissionId = UUID.randomUUID();

        send("SubmissionReceived", received(UUID.randomUUID(), portalUser, submissionId, "CLAIM", region));

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(submissions.find(submissionId)).hasValueSatisfying(submission -> {
                    assertThat(submission.getTeamId()).isEqualTo(claimsEu);
                    assertThat(submission.getStatus().name()).isEqualTo("RECEIVED");
                }));
    }

    @Test
    void submissionMatchingNoRuleGoesToTheDefaultTeam() {
        UUID submissionId = UUID.randomUUID();

        send("SubmissionReceived", received(UUID.randomUUID(), portalUser, submissionId, "CLAIM", region));

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(submissions.find(submissionId))
                        .hasValueSatisfying(
                                submission -> assertThat(submission.getTeamId()).isEqualTo(defaultTeam)));
    }

    @Test
    void redeliveredEventCreatesOneSubmission() {
        UUID eventId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        String event = received(eventId, portalUser, submissionId, "CLAIM", region);
        UUID marker = UUID.randomUUID();

        send("SubmissionReceived", event);
        send("SubmissionReceived", event);
        // Same key, so the same partition: once the marker is in, both copies have been handled
        send("SubmissionReceived", received(UUID.randomUUID(), portalUser, marker, "CLAIM", region));

        await().atMost(Duration.ofSeconds(30))
                .until(() -> submissions.find(marker).isPresent());
        assertThat(repository.count(
                        (root, query, cb) -> cb.equal(root.get("portalUser").get("id"), portalUser)))
                .isEqualTo(2);
    }

    @Test
    void unknownSchemaVersionGoesToTheDeadLetterTopicWithTheReason() {
        UUID submissionId = UUID.randomUUID();
        String event = received(UUID.randomUUID(), portalUser, submissionId, "CLAIM", region)
                .replace("\"schemaVersion\":1", "\"schemaVersion\":99");

        send("SubmissionReceived", event);

        var dead = TestKafka.await(
                kafkaContainer,
                "portal-intake-dlq",
                payload -> payload.contains(submissionId.toString()),
                Duration.ofSeconds(30));
        assertThat(dead).hasSize(1);
        assertThat(new String(
                        dead.getFirst()
                                .headers()
                                .lastHeader("kafka_dlt-exception-message")
                                .value(),
                        StandardCharsets.UTF_8))
                .contains("schemaVersion 99");
        assertThat(submissions.find(submissionId)).isEmpty();
    }

    @Test
    void eventWithoutTypeHeaderGoesToTheDeadLetterTopic() {
        UUID submissionId = UUID.randomUUID();

        send(null, received(UUID.randomUUID(), portalUser, submissionId, "CLAIM", region));

        assertThat(TestKafka.await(
                        kafkaContainer,
                        "portal-intake-dlq",
                        payload -> payload.contains(submissionId.toString()),
                        Duration.ofSeconds(30)))
                .hasSize(1);
    }
}

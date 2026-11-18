package com.bnaqica.crmdemocoreservice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bnaqica.crmdemocoreservice.TestcontainersConfiguration;
import com.bnaqica.crmdemocoreservice.entity.PortalUser;
import com.bnaqica.crmdemocoreservice.entity.ProcessedEvent;
import com.bnaqica.crmdemocoreservice.entity.RoutingRule;
import com.bnaqica.crmdemocoreservice.entity.Submission;
import com.bnaqica.crmdemocoreservice.entity.SubmissionDocument;
import com.bnaqica.crmdemocoreservice.entity.SubmissionStatus;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/** The V3 migration's tables, through their repositories, on real PostgreSQL. */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class SubmissionRepositoryTests {

    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");
    private static final UUID TEAM = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    @Autowired
    private SubmissionRepository submissions;

    @Autowired
    private PortalUserRepository portalUsers;

    @Autowired
    private RoutingRuleRepository routingRules;

    @Autowired
    private ProcessedEventRepository processedEvents;

    @Autowired
    private EntityManager entityManager;

    @Test
    void storesASubmissionWithItsDocumentsAndSubmitter() {
        PortalUser user = portalUsers.save(PortalUser.builder()
                .id(UUID.randomUUID())
                .email("ana@acme.example")
                .updatedAt(NOW)
                .build());
        UUID id = UUID.randomUUID();
        submissions.save(Submission.builder()
                .id(id)
                .portalUser(user)
                .version(1)
                .type("CLAIM")
                .region("EU")
                .teamId(TEAM)
                .status(SubmissionStatus.RECEIVED)
                .receivedAt(NOW)
                .documents(List.of(new SubmissionDocument(
                        UUID.randomUUID(), "u/1/invoice.pdf", "application/pdf", 1234, "a".repeat(64))))
                .build());
        entityManager.flush();
        entityManager.clear();

        Submission found = submissions.findById(id).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(SubmissionStatus.RECEIVED);
        assertThat(found.getPortalUser().getEmail()).isEqualTo("ana@acme.example");
        assertThat(found.getDocuments()).singleElement().satisfies(document -> {
            assertThat(document.getStorageKey()).isEqualTo("u/1/invoice.pdf");
            assertThat(document.getSize()).isEqualTo(1234);
        });
    }

    @Test
    void routingRulesComeBackInPositionOrder() {
        routingRules.save(rule(2, "CLAIM"));
        routingRules.save(rule(1, "INVOICE"));
        entityManager.flush();

        assertThat(routingRules.findAllByOrderByPositionAsc())
                .extracting(RoutingRule::getType)
                .containsExactly("INVOICE", "CLAIM");
    }

    @Test
    void anEventIdCanBeRecordedOnlyOnce() {
        UUID eventId = UUID.randomUUID();
        processedEvents.saveAndFlush(new ProcessedEvent(eventId, NOW));
        entityManager.clear();

        assertThatThrownBy(() -> processedEvents.saveAndFlush(new ProcessedEvent(eventId, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static RoutingRule rule(int position, String type) {
        return RoutingRule.builder()
                .id(UUID.randomUUID())
                .position(position)
                .type(type)
                .region("EU")
                .teamId(TEAM)
                .build();
    }
}

package com.bnaqica.crmdemocoreservice.publisher;

import static org.assertj.core.api.Assertions.assertThat;

import com.bnaqica.crmdemocoreservice.service.OutboxService;
import com.bnaqica.crmdemocoreservice.support.IntegrationTest;
import com.bnaqica.crmdemocoreservice.support.TestKafka;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.kafka.KafkaContainer;

class OutboxIntegrationTests extends IntegrationTest {

    @Autowired
    private OutboxService outbox;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private KafkaContainer kafka;

    @Test
    void committedEventIsPublished() {
        String marker = UUID.randomUUID().toString();
        transactions.executeWithoutResult(
                status -> outbox.add("audit-events", "k1", "{\"marker\":\"" + marker + "\"}"));

        var records =
                TestKafka.await(kafka, "audit-events", payload -> payload.contains(marker), Duration.ofSeconds(30));

        assertThat(records).hasSize(1);
        assertThat(records.getFirst().key()).isEqualTo("k1");
    }

    @Test
    void rolledBackEventIsNotPublished() {
        String marker = UUID.randomUUID().toString();
        transactions.executeWithoutResult(status -> {
            outbox.add("audit-events", "k1", "{\"marker\":\"" + marker + "\"}");
            status.setRollbackOnly();
        });

        var records =
                TestKafka.await(kafka, "audit-events", payload -> payload.contains(marker), Duration.ofSeconds(5));

        assertThat(records).isEmpty();
    }
}

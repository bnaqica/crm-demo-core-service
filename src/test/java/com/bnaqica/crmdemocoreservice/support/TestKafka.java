package com.bnaqica.crmdemocoreservice.support;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.testcontainers.kafka.KafkaContainer;

/** Reads what the application published, from the beginning of a topic, with a throwaway consumer group. */
public final class TestKafka {

    private TestKafka() {}

    /** Polls {@code topic} until a record matches or {@code wait} is up; returns the matching records. */
    public static List<ConsumerRecord<String, String>> await(
            KafkaContainer kafka, String topic, Predicate<String> payloadMatches, Duration wait) {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest");
        List<ConsumerRecord<String, String>> matching = new ArrayList<>();
        try (var consumer = new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(topic));
            long deadline = System.nanoTime() + wait.toNanos();
            while (matching.isEmpty() && System.nanoTime() < deadline) {
                consumer.poll(Duration.ofMillis(500)).forEach(record -> {
                    if (payloadMatches.test(record.value())) {
                        matching.add(record);
                    }
                });
            }
        }
        return matching;
    }
}

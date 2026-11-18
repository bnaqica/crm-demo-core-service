package com.bnaqica.crmdemocoreservice.config;

import com.bnaqica.crmdemocoreservice.exception.InvalidEventException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

/**
 * Listener errors are retried 3 times a second apart, then the record goes to {@code <topic>-dlq} with the reason in
 * its {@code kafka_dlt-exception-*} headers. Invalid and unreadable events go there at once. Every dead-lettered
 * record is logged first, so failures never disappear silently.
 */
@Slf4j
@Configuration
public class KafkaConfig {

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> kafka) {
        var deadLetter = new DeadLetterPublishingRecoverer(
                kafka, (record, ex) -> new TopicPartition(record.topic() + "-dlq", -1));
        var handler = new DefaultErrorHandler(
                (record, ex) -> {
                    log.error(
                            "Sending record {}-{}@{} to {}-dlq",
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            record.topic(),
                            ex);
                    deadLetter.accept(record, ex);
                },
                new FixedBackOff(1000, 3));
        handler.addNotRetryableExceptions(InvalidEventException.class, JacksonException.class);
        return handler;
    }
}

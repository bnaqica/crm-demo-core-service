package com.bnaqica.crmdemocoreservice.exception;

import java.util.UUID;
import lombok.Getter;

/** Kafka refused or didn't acknowledge an outbox event. */
@Getter
public class OutboxPublishException extends RuntimeException {

    private final UUID eventId;
    private final String topic;

    public OutboxPublishException(UUID eventId, String topic, Throwable cause) {
        super("Couldn't publish outbox event " + eventId + " to " + topic, cause);
        this.eventId = eventId;
        this.topic = topic;
    }
}

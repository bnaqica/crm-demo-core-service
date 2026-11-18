package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.entity.OutboxEvent;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** CRUD for {@code outbox_event}. Every method joins the caller's transaction and fails without one. */
public interface OutboxService {

    void add(String topic, String key, String payload);

    /** The oldest event to publish, locked for this transaction; empty when there is none. */
    Optional<OutboxEvent> lockNextUnpublished();

    void markPublished(OutboxEvent event, Instant at);

    /**
     * Counts a failed attempt and parks the event once it has failed too often.
     *
     * @return whether the event is now parked
     */
    boolean recordFailure(UUID id, String error);
}

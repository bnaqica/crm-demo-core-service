package com.bnaqica.crmdemocoreservice.repository;

import com.bnaqica.crmdemocoreservice.entity.OutboxEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /** The oldest event that is neither published nor parked, locked so another instance's relay skips it. */
    @Query(
            value = "SELECT * FROM {h-schema}outbox_event WHERE published_at IS NULL AND parked_at IS NULL"
                    + " ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    Optional<OutboxEvent> lockNextUnpublished();
}

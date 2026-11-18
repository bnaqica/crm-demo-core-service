package com.bnaqica.crmdemocoreservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

/**
 * An event id already handled, written in the same transaction as its effect. Always new, so saving a second copy
 * is an INSERT that the primary key rejects, rather than a silent merge.
 */
@Entity
@Table(name = "processed_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA
@AllArgsConstructor
public class ProcessedEvent implements Persistable<UUID> {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Override
    public UUID getId() {
        return eventId;
    }

    @Override
    public boolean isNew() {
        return true;
    }
}

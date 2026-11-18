package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.entity.OutboxEvent;
import com.bnaqica.crmdemocoreservice.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY) // an outbox write outside the change's transaction is a bug
public class OutboxServiceImpl implements OutboxService {

    static final int MAX_ATTEMPTS = 5;

    private final OutboxEventRepository repository;
    private final Clock clock;

    @Override
    public void add(String topic, String key, String payload) {
        repository.save(OutboxEvent.builder()
                .id(UUID.randomUUID())
                .topic(topic)
                .messageKey(key)
                .payload(payload)
                .createdAt(Instant.now(clock))
                .build());
    }

    @Override
    public Optional<OutboxEvent> lockNextUnpublished() {
        return repository.lockNextUnpublished();
    }

    @Override
    public void markPublished(OutboxEvent event, Instant at) {
        event.setPublishedAt(at);
        repository.save(event);
    }

    @Override
    public boolean recordFailure(UUID id, String error) {
        OutboxEvent event = repository.findById(id).orElseThrow();
        event.setAttempts(event.getAttempts() + 1);
        event.setLastError(error);
        if (event.getAttempts() >= MAX_ATTEMPTS) {
            event.setParkedAt(Instant.now(clock));
        }
        repository.save(event);
        return event.getParkedAt() != null;
    }
}

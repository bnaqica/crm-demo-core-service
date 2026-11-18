package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.entity.ProcessedEvent;
import com.bnaqica.crmdemocoreservice.repository.ProcessedEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The insert is the guard: two consumers racing on one event id can't both commit, because the primary key rejects
 * the second; its transaction rolls back and the retry then sees the id and skips.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ProcessedEventServiceImpl implements ProcessedEventService {

    private final ProcessedEventRepository repository;
    private final Clock clock;

    @Override
    public boolean markProcessed(UUID eventId) {
        if (repository.existsById(eventId)) {
            return false;
        }
        repository.saveAndFlush(new ProcessedEvent(eventId, Instant.now(clock)));
        return true;
    }
}

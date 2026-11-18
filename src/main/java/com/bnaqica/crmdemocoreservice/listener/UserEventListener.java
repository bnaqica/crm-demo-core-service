package com.bnaqica.crmdemocoreservice.listener;

import com.bnaqica.crmdemocoreservice.service.TeamScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Hands each message on {@code ums-user-events} to {@link TeamScopeService}. Every instance caches scopes of its own,
 * so each one consumes in a consumer group of its own, from the latest offset (older events can't concern its cache).
 */
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final TeamScopeService teamScope;

    @KafkaListener(
            topics = "${crm.topics.user-events}",
            groupId = "${spring.application.name}-scope-${random.uuid}",
            properties = "auto.offset.reset=latest")
    void on(String payload) {
        teamScope.onUserEvent(payload);
    }
}

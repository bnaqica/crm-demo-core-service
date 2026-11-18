package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.config.CrmProperties;
import com.bnaqica.crmdemocoreservice.dto.event.AuditAction;
import com.bnaqica.crmdemocoreservice.dto.event.AuditEvent;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
public class AuthorizationAuditServiceImpl implements AuthorizationAuditService {

    static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ATTRIBUTE =
            AuthorizationAuditServiceImpl.class.getName() + ".correlationId";
    private static final String ANONYMOUS = "anonymous";

    private final OutboxService outbox;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    private final TransactionTemplate ownTransaction;
    private final String topic;

    @Autowired
    public AuthorizationAuditServiceImpl(
            OutboxService outbox,
            JsonMapper jsonMapper,
            Clock clock,
            PlatformTransactionManager transactionManager,
            CrmProperties properties) {
        this(outbox, jsonMapper, clock, transactionManager, properties.topics().auditEvents());
    }

    AuthorizationAuditServiceImpl(
            OutboxService outbox,
            JsonMapper jsonMapper,
            Clock clock,
            PlatformTransactionManager transactionManager,
            String topic) {
        this.outbox = outbox;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
        this.ownTransaction = new TransactionTemplate(transactionManager);
        this.ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.topic = topic;
    }

    @Override
    public void denied(String permission, String reason) {
        AuditEvent event = event(AuditAction.AUTHORIZATION_DENIED)
                .outcome("DENIED")
                .permission(permission)
                .targetType("ENDPOINT")
                .reason(reason)
                .build();
        log.warn("Denied {} to {}: {} (permission {})", event.path(), event.actorId(), reason, permission);
        publish(event);
    }

    @Override
    public void crossTeamRead(UUID teamId, String recordType, String recordId) {
        publish(event(AuditAction.CROSS_TEAM_READ)
                .outcome("SUCCESS")
                .teamId(teamId)
                .targetType(recordType)
                .targetId(recordId)
                .build());
    }

    private AuditEvent.AuditEventBuilder event(AuditAction action) {
        HttpServletRequest request = CurrentUser.request().orElse(null);
        String path = request == null ? null : request.getMethod() + " " + request.getRequestURI();
        return AuditEvent.builder()
                .eventId(UUID.randomUUID())
                .schemaVersion(1)
                .occurredAt(Instant.now(clock))
                .source("CORE")
                .realm("crm-internal")
                .actorId(CurrentUser.token().map(Jwt::getSubject).orElse(ANONYMOUS))
                .actorType("USER")
                .action(action)
                .path(path)
                .targetId(path) // replaced by the record for record-level events
                .ipAddress(request == null ? null : request.getRemoteAddr())
                .correlationId(correlationId(request));
    }

    private void publish(AuditEvent event) {
        try {
            ownTransaction.executeWithoutResult(
                    status -> outbox.add(topic, event.actorId(), jsonMapper.writeValueAsString(event)));
        } catch (RuntimeException ex) {
            // Never turn the request's own outcome into an audit failure
            log.error("Couldn't write audit event {} {}", event.action(), event.eventId(), ex);
        }
    }

    /** The caller's {@code X-Correlation-Id}, or one made up once per request so all its events share it. */
    private static String correlationId(HttpServletRequest request) {
        if (request == null) {
            return UUID.randomUUID().toString();
        }
        Object existing = request.getAttribute(CORRELATION_ATTRIBUTE);
        if (existing != null) {
            return existing.toString();
        }
        String header = request.getHeader(CORRELATION_HEADER);
        String id = header == null || header.isBlank() ? UUID.randomUUID().toString() : header;
        request.setAttribute(CORRELATION_ATTRIBUTE, id);
        return id;
    }
}

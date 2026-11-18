package com.bnaqica.crmdemocoreservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class AuthorizationAuditServiceImplTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID TEAM_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private final OutboxService outbox = mock(OutboxService.class);
    private final AuthorizationAuditService audit = new AuthorizationAuditServiceImpl(
            outbox,
            JSON,
            Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC),
            mock(PlatformTransactionManager.class),
            "audit-events");

    @BeforeEach
    void signedInRequest() {
        var request = new MockHttpServletRequest("POST", "/submissions/42/accept");
        request.addHeader("X-Correlation-Id", "corr-1");
        request.setRemoteAddr("10.0.0.7");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u1").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void denialIsPublishedWithUserPermissionAndPath() {
        audit.denied("submissions:write", "missing permission");

        JsonNode event = published("u1");
        assertThat(event.get("action").asString()).isEqualTo("AUTHORIZATION_DENIED");
        assertThat(event.get("outcome").asString()).isEqualTo("DENIED");
        assertThat(event.get("actorId").asString()).isEqualTo("u1");
        assertThat(event.get("permission").asString()).isEqualTo("submissions:write");
        assertThat(event.get("path").asString()).isEqualTo("POST /submissions/42/accept");
        assertThat(event.get("reason").asString()).isEqualTo("missing permission");
        assertThat(event.get("source").asString()).isEqualTo("CORE");
        assertThat(event.get("realm").asString()).isEqualTo("crm-internal");
        assertThat(event.get("correlationId").asString()).isEqualTo("corr-1");
        assertThat(event.get("ipAddress").asString()).isEqualTo("10.0.0.7");
        assertThat(event.get("occurredAt").asString()).isEqualTo("2026-11-12T10:00:00Z");
        assertThat(event.get("eventId").asString()).isNotBlank();
        assertThat(event.get("schemaVersion").asInt()).isEqualTo(1);
    }

    @Test
    void crossTeamReadIsPublishedWithTheRecordAndTeam() {
        audit.crossTeamRead(TEAM_B, "SUBMISSION", "42");

        JsonNode event = published("u1");
        assertThat(event.get("action").asString()).isEqualTo("CROSS_TEAM_READ");
        assertThat(event.get("outcome").asString()).isEqualTo("SUCCESS");
        assertThat(event.get("targetType").asString()).isEqualTo("SUBMISSION");
        assertThat(event.get("targetId").asString()).isEqualTo("42");
        assertThat(event.get("teamId").asString()).isEqualTo(TEAM_B.toString());
        assertThat(event.get("path").asString()).isEqualTo("POST /submissions/42/accept");
    }

    private JsonNode published(String key) {
        var payload = ArgumentCaptor.forClass(String.class);
        verify(outbox).add(eq("audit-events"), eq(key), payload.capture());
        return JSON.readTree(payload.getValue());
    }
}

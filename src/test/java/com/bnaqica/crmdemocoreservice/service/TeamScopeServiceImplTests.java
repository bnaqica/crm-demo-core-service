package com.bnaqica.crmdemocoreservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bnaqica.crmdemocoreservice.client.UmsClient;
import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import com.bnaqica.crmdemocoreservice.exception.UmsClientException;
import com.bnaqica.crmdemocoreservice.exception.UmsUnavailableException;
import com.bnaqica.crmdemocoreservice.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.json.JsonMapper;

class TeamScopeServiceImplTests {

    private static final UUID TEAM_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final TeamScope IN_A = new TeamScope("u1", "ACTIVE", Set.of(TEAM_A), Set.of(), false);
    private static final TeamScope IN_NO_TEAM = new TeamScope("u1", "ACTIVE", Set.of(), Set.of(), false);

    private final UmsClient ums = mock(UmsClient.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-12T10:00:00Z"));
    private final TeamScopeService service =
            new TeamScopeServiceImpl(ums, JsonMapper.builder().build(), clock, Duration.ofMinutes(5));

    @Test
    void cachedScopeIsReusedWithinTtl() {
        when(ums.teamScope("u1")).thenReturn(IN_A);

        service.cached("u1");
        clock.advance(Duration.ofMinutes(4));
        TeamScope scope = service.cached("u1");

        assertThat(scope).isEqualTo(IN_A);
        verify(ums, times(1)).teamScope("u1");
    }

    @Test
    void cachedScopeExpiresAfterFiveMinutes() {
        when(ums.teamScope("u1")).thenReturn(IN_A, IN_NO_TEAM);

        service.cached("u1");
        clock.advance(Duration.ofMinutes(5).plusSeconds(1));

        assertThat(service.cached("u1")).isEqualTo(IN_NO_TEAM);
    }

    @Test
    void liveScopeAlwaysAsksUmsAndRefreshesCache() {
        when(ums.teamScope("u1")).thenReturn(IN_A, IN_NO_TEAM);

        service.cached("u1");
        assertThat(service.live("u1")).isEqualTo(IN_NO_TEAM);
        assertThat(service.cached("u1")).isEqualTo(IN_NO_TEAM);
        verify(ums, times(2)).teamScope("u1");
    }

    @Test
    void membershipEventEvictsTheUsersScope() {
        when(ums.teamScope("u1")).thenReturn(IN_A, IN_NO_TEAM);
        service.cached("u1");

        service.onUserEvent("""
                {"type":"TeamMembershipChanged","eventId":"%s","schemaVersion":1,"sub":"u1","teamId":"%s"}
                """.formatted(UUID.randomUUID(), TEAM_A));

        assertThat(service.cached("u1").memberTeams()).isEmpty();
    }

    @Test
    void eventForAnotherUserKeepsTheCache() {
        when(ums.teamScope("u1")).thenReturn(IN_A, IN_NO_TEAM);
        service.cached("u1");

        service.onUserEvent("{\"type\":\"TeamMembershipChanged\",\"sub\":\"u2\"}");

        assertThat(service.cached("u1")).isEqualTo(IN_A);
    }

    @Test
    void eventWithoutSubIsIgnored() {
        service.onUserEvent("{\"type\":\"SomethingElse\"}");
    }

    @Test
    void unknownUserIsNotActive() {
        when(ums.teamScope("u1")).thenThrow(new UmsClientException(404, "not found"));

        assertThat(service.cached("u1").isActive()).isFalse();
    }

    @Test
    void readUsesCachedScopeWhileUmsIsDown() {
        when(ums.teamScope("u1")).thenReturn(IN_A).thenThrow(new ResourceAccessException("down"));
        service.cached("u1");
        clock.advance(Duration.ofMinutes(1));

        assertThat(service.cached("u1")).isEqualTo(IN_A);
    }

    @Test
    void readWithoutCachedScopeFailsWhileUmsIsDown() {
        when(ums.teamScope("u1")).thenThrow(new ResourceAccessException("down"));

        assertThatThrownBy(() -> service.cached("u1")).isInstanceOf(UmsUnavailableException.class);
    }

    @Test
    void liveScopeFailsClosedWhileUmsIsDownEvenWithCache() {
        when(ums.teamScope("u1")).thenReturn(IN_A).thenThrow(new UmsClientException(500, "boom"));
        service.cached("u1");

        assertThatThrownBy(() -> service.live("u1")).isInstanceOf(UmsUnavailableException.class);
    }

    @Test
    void inactiveStatusIsNotCachedSoAReEnabledUserGetsInAtOnce() {
        when(ums.teamScope("u1"))
                .thenReturn(new TeamScope("u1", "INVITED", Set.of(), Set.of(), false))
                .thenReturn(IN_A);

        assertThat(service.cached("u1").isActive()).isFalse();
        assertThat(service.cached("u1")).isEqualTo(IN_A);
    }

    @Test
    void evictionDuringAFetchKeepsTheOldScopeOut() throws Exception {
        var fetching = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(ums.teamScope("u1"))
                .thenAnswer(invocation -> {
                    fetching.countDown();
                    release.await(5, TimeUnit.SECONDS);
                    return IN_A; // the scope as it was before the membership change
                })
                .thenReturn(IN_NO_TEAM);

        var read = CompletableFuture.supplyAsync(() -> service.cached("u1"));
        assertThat(fetching.await(5, TimeUnit.SECONDS)).isTrue();
        var evict = CompletableFuture.runAsync(() -> service.onUserEvent("{\"sub\":\"u1\"}"));
        try {
            evict.get(200, TimeUnit.MILLISECONDS); // may finish at once, or wait for the fetch
        } catch (TimeoutException expected) {
            // waiting for the in-flight fetch, as it should
        }
        release.countDown();
        read.get(5, TimeUnit.SECONDS);
        evict.get(5, TimeUnit.SECONDS);

        assertThat(service.cached("u1")).isEqualTo(IN_NO_TEAM);
    }
}

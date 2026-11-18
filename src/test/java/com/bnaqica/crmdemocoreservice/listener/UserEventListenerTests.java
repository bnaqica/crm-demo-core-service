package com.bnaqica.crmdemocoreservice.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import com.bnaqica.crmdemocoreservice.service.TeamScopeService;
import com.bnaqica.crmdemocoreservice.support.IntegrationTest;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

class UserEventListenerTests extends IntegrationTest {

    private static final UUID TEAM_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    @Autowired
    private TeamScopeService teamScope;

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @Test
    void userRemovedFromTeamLosesItAfterTheMembershipEvent() {
        String sub = "removed-" + UUID.randomUUID();
        when(ums.teamScope(sub))
                .thenReturn(
                        new TeamScope(sub, "ACTIVE", Set.of(TEAM_A), Set.of(), false),
                        new TeamScope(sub, "ACTIVE", Set.of(), Set.of(), false));
        assertThat(teamScope.cached(sub).memberTeams()).containsExactly(TEAM_A);

        String event = """
                {"type":"TeamMembershipChanged","eventId":"%s","schemaVersion":1,"sub":"%s","teamId":"%s"}
                """.formatted(UUID.randomUUID(), sub, TEAM_A);
        // The listener starts at the latest offset, so keep sending until its consumer has joined and evicted
        await().atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    kafka.send("ums-user-events", sub, event);
                    assertThat(teamScope.cached(sub).memberTeams()).isEmpty();
                });
    }
}

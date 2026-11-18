package com.bnaqica.crmdemocoreservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bnaqica.crmdemocoreservice.dto.TeamFilter;
import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeamAccessPolicyTests {

    private static final UUID TEAM_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID TEAM_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private static final TeamScope MEMBER_OF_A = new TeamScope("member", "ACTIVE", Set.of(TEAM_A), Set.of(), false);
    private static final TeamScope MANAGER_OF_A =
            new TeamScope("manager", "ACTIVE", Set.of(TEAM_A), Set.of(TEAM_A), false);
    private static final TeamScope SUPER_MANAGER_IN_A =
            new TeamScope("super", "ACTIVE", Set.of(TEAM_A), Set.of(), true);

    private final AccessPolicy policy = new TeamAccessPolicy();

    @Test
    void memberCanViewOwnTeam() {
        assertThat(policy.canView(MEMBER_OF_A, TEAM_A)).isTrue();
    }

    @Test
    void memberCannotViewOtherTeam() {
        assertThat(policy.canView(MEMBER_OF_A, TEAM_B)).isFalse();
    }

    @Test
    void superManagerCanViewAnyTeam() {
        assertThat(policy.canView(SUPER_MANAGER_IN_A, TEAM_B)).isTrue();
    }

    @Test
    void memberCanEditOwnTeam() {
        assertThat(policy.canEdit(MEMBER_OF_A, TEAM_A)).isTrue();
    }

    @Test
    void superManagerCannotEditTeamTheyAreNotIn() {
        assertThat(policy.canEdit(SUPER_MANAGER_IN_A, TEAM_B)).isFalse();
    }

    @Test
    void superManagerCanEditOwnTeam() {
        assertThat(policy.canEdit(SUPER_MANAGER_IN_A, TEAM_A)).isTrue();
    }

    @Test
    void memberWhoDoesNotManageCannotOpenManagerView() {
        assertThat(policy.canManage(MEMBER_OF_A, TEAM_A)).isFalse();
    }

    @Test
    void managerCanOpenManagerViewOfManagedTeam() {
        assertThat(policy.canManage(MANAGER_OF_A, TEAM_A)).isTrue();
    }

    @Test
    void managerCannotOpenManagerViewOfOtherTeam() {
        assertThat(policy.canManage(MANAGER_OF_A, TEAM_B)).isFalse();
    }

    @Test
    void superManagerCanOpenAnyManagerView() {
        assertThat(policy.canManage(SUPER_MANAGER_IN_A, TEAM_B)).isTrue();
    }

    @Test
    void memberListsOnlyOwnTeams() {
        assertThat(policy.scopeFilter(MEMBER_OF_A)).isEqualTo(TeamFilter.only(Set.of(TEAM_A)));
    }

    @Test
    void superManagerListsAllTeams() {
        assertThat(policy.scopeFilter(SUPER_MANAGER_IN_A).allTeams()).isTrue();
    }

    @Test
    void userInNoTeamListsNothing() {
        var nobody = new TeamScope("nobody", "ACTIVE", Set.of(), Set.of(), false);
        TeamFilter filter = policy.scopeFilter(nobody);
        assertThat(filter.allTeams()).isFalse();
        assertThat(filter.teams()).isEmpty();
    }
}

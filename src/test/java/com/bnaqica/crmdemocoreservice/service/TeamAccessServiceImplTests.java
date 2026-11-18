package com.bnaqica.crmdemocoreservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bnaqica.crmdemocoreservice.dto.TeamFilter;
import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import com.bnaqica.crmdemocoreservice.exception.RecordNotFoundException;
import com.bnaqica.crmdemocoreservice.exception.TeamAccessDeniedException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** The team-scope-enforcement scenarios, as the services that own team records see them. */
class TeamAccessServiceImplTests {

    private static final UUID TEAM_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID TEAM_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private final TeamScopeService teamScope = mock(TeamScopeService.class);
    private final AuthorizationAuditService audit = mock(AuthorizationAuditService.class);
    private final TeamAccessService access = new TeamAccessServiceImpl(teamScope, new TeamAccessPolicy(), audit);

    @BeforeEach
    void signIn() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u1").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    private void memberOfA() {
        var scope = new TeamScope("u1", "ACTIVE", Set.of(TEAM_A), Set.of(), false);
        when(teamScope.cached("u1")).thenReturn(scope);
        when(teamScope.live("u1")).thenReturn(scope);
    }

    private void superManagerInA() {
        var scope = new TeamScope("u1", "ACTIVE", Set.of(TEAM_A), Set.of(), true);
        when(teamScope.cached("u1")).thenReturn(scope);
        when(teamScope.live("u1")).thenReturn(scope);
    }

    @Test
    void memberReadingAnotherTeamsRecordGetsNotFound() {
        memberOfA();

        assertThatThrownBy(() -> access.checkView(TEAM_B, "SUBMISSION", "42"))
                .isInstanceOf(RecordNotFoundException.class);
        verify(audit, never()).crossTeamRead(any(), anyString(), anyString());
    }

    @Test
    void memberReadingOwnTeamsRecordIsNotAudited() {
        memberOfA();

        access.checkView(TEAM_A, "SUBMISSION", "42");

        verify(audit, never()).crossTeamRead(any(), anyString(), anyString());
    }

    @Test
    void superManagerReadsAnotherTeamAndIsAudited() {
        superManagerInA();

        access.checkView(TEAM_B, "SUBMISSION", "42");

        verify(audit).crossTeamRead(TEAM_B, "SUBMISSION", "42");
    }

    @Test
    void memberListsOnlyOwnTeam() {
        memberOfA();

        assertThat(access.readableTeams()).isEqualTo(TeamFilter.only(Set.of(TEAM_A)));
    }

    @Test
    void superManagerEditingAnotherTeamGets403AndIsAudited() {
        superManagerInA();

        assertThatThrownBy(() -> access.checkEdit(TEAM_B)).isInstanceOf(TeamAccessDeniedException.class);
        verify(audit).denied(any(), anyString());
    }

    @Test
    void editUsesLiveScope() {
        when(teamScope.cached("u1")).thenReturn(new TeamScope("u1", "ACTIVE", Set.of(TEAM_A), Set.of(), false));
        when(teamScope.live("u1")).thenReturn(new TeamScope("u1", "ACTIVE", Set.of(), Set.of(), false));

        assertThatThrownBy(() -> access.checkEdit(TEAM_A)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void memberEditsOwnTeam() {
        memberOfA();

        access.checkEdit(TEAM_A);
    }

    @Test
    void memberOpeningManagerViewGets403() {
        memberOfA();

        assertThatThrownBy(() -> access.checkManage(TEAM_A)).isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void changingARecordOutsideTheScopeIsNotFound() {
        memberOfA();

        assertThatThrownBy(() -> access.checkChange(TEAM_B, "SUBMISSION", "42"))
                .isInstanceOf(RecordNotFoundException.class);
    }

    @Test
    void superManagerChangingAnotherTeamsRecordGets403() {
        superManagerInA();

        assertThatThrownBy(() -> access.checkChange(TEAM_B, "SUBMISSION", "42"))
                .isInstanceOf(TeamAccessDeniedException.class);
        verify(audit).denied(any(), anyString());
        verify(audit, never()).crossTeamRead(any(), anyString(), anyString());
    }

    @Test
    void memberChangesOwnTeamsRecordUsingLiveScope() {
        when(teamScope.cached("u1")).thenReturn(new TeamScope("u1", "ACTIVE", Set.of(), Set.of(), false));
        when(teamScope.live("u1")).thenReturn(new TeamScope("u1", "ACTIVE", Set.of(TEAM_A), Set.of(), false));

        access.checkChange(TEAM_A, "SUBMISSION", "42");
    }
}

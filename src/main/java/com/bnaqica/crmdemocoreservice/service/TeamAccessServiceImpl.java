package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.dto.TeamFilter;
import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import com.bnaqica.crmdemocoreservice.exception.RecordNotFoundException;
import com.bnaqica.crmdemocoreservice.exception.TeamAccessDeniedException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamAccessServiceImpl implements TeamAccessService {

    private final TeamScopeService teamScope;
    private final AccessPolicy policy;
    private final AuthorizationAuditService audit;

    @Override
    public TeamFilter readableTeams() {
        return policy.scopeFilter(teamScope.cached(CurrentUser.sub()));
    }

    @Override
    public void checkView(UUID teamId, String recordType, String recordId) {
        TeamScope scope = teamScope.cached(CurrentUser.sub());
        if (!policy.canView(scope, teamId)) {
            throw new RecordNotFoundException(recordType, recordId);
        }
        if (!scope.memberTeams().contains(teamId)) {
            audit.crossTeamRead(teamId, recordType, recordId);
        }
    }

    @Override
    public void checkEdit(UUID teamId) {
        if (!policy.canEdit(teamScope.live(CurrentUser.sub()), teamId)) {
            deny("not a member of team " + teamId);
        }
    }

    @Override
    public void checkChange(UUID teamId, String recordType, String recordId) {
        TeamScope scope = teamScope.live(CurrentUser.sub());
        if (!policy.canView(scope, teamId)) {
            throw new RecordNotFoundException(recordType, recordId);
        }
        if (!policy.canEdit(scope, teamId)) {
            deny("not a member of team " + teamId);
        }
    }

    @Override
    public void checkManage(UUID teamId) {
        if (!policy.canManage(teamScope.cached(CurrentUser.sub()), teamId)) {
            deny("not a manager of team " + teamId);
        }
    }

    private void deny(String reason) {
        audit.denied(null, reason);
        throw new TeamAccessDeniedException(reason);
    }
}

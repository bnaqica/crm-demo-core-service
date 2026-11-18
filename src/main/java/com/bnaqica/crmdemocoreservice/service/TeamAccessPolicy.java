package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.dto.TeamFilter;
import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Members read and change their teams' records. Super Managers ({@code allTeams}) also read every other team's, but
 * change only their own. Manager views are for the team's managers and Super Managers.
 */
@Service
public class TeamAccessPolicy implements AccessPolicy {

    @Override
    public boolean canView(TeamScope scope, UUID teamId) {
        return scope.allTeams() || scope.memberTeams().contains(teamId);
    }

    @Override
    public boolean canEdit(TeamScope scope, UUID teamId) {
        return scope.memberTeams().contains(teamId);
    }

    @Override
    public boolean canManage(TeamScope scope, UUID teamId) {
        return scope.allTeams() || scope.managedTeams().contains(teamId);
    }

    @Override
    public TeamFilter scopeFilter(TeamScope scope) {
        return scope.allTeams() ? TeamFilter.all() : TeamFilter.only(scope.memberTeams());
    }
}

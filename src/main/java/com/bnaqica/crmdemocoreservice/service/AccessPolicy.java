package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.dto.TeamFilter;
import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import java.util.UUID;

/** Which team-owned records a user may read, change or manage. Pure rules; no lookups. */
public interface AccessPolicy {

    boolean canView(TeamScope scope, UUID teamId);

    boolean canEdit(TeamScope scope, UUID teamId);

    boolean canManage(TeamScope scope, UUID teamId);

    TeamFilter scopeFilter(TeamScope scope);
}

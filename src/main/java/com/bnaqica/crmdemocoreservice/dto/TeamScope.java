package com.bnaqica.crmdemocoreservice.dto;

import java.util.Set;
import java.util.UUID;

/**
 * Which teams a user belongs to and manages, from the UMS ({@code GET /internal/users/{sub}/team-scope}).
 *
 * @param status the UMS account status; only {@code ACTIVE} users get access
 * @param allTeams true for Super Managers, who may read every team's records
 */
public record TeamScope(String sub, String status, Set<UUID> memberTeams, Set<UUID> managedTeams, boolean allTeams) {

    public static final String ACTIVE = "ACTIVE";

    public TeamScope {
        memberTeams = memberTeams == null ? Set.of() : Set.copyOf(memberTeams);
        managedTeams = managedTeams == null ? Set.of() : Set.copyOf(managedTeams);
    }

    /** A user the UMS has no internal account for. */
    public static TeamScope unknown(String sub) {
        return new TeamScope(sub, "UNKNOWN", Set.of(), Set.of(), false);
    }

    public boolean isActive() {
        return ACTIVE.equals(status);
    }
}

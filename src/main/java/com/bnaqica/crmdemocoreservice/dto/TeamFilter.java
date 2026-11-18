package com.bnaqica.crmdemocoreservice.dto;

import java.util.Set;
import java.util.UUID;

/** The teams a list query may return records from: all of them, or only {@code teams}. */
public record TeamFilter(boolean allTeams, Set<UUID> teams) {

    public static TeamFilter all() {
        return new TeamFilter(true, Set.of());
    }

    public static TeamFilter only(Set<UUID> teams) {
        return new TeamFilter(false, Set.copyOf(teams));
    }
}

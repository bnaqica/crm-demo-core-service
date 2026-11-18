package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.dto.TeamScope;

/**
 * A user's team scope and status from the UMS. Reads use a cached copy (at most 5 minutes old, evicted on the user's
 * UMS events); writes use {@link #live}. A user the UMS doesn't know comes back with status {@code UNKNOWN}.
 */
public interface TeamScopeService {

    /** @throws com.bnaqica.crmdemocoreservice.exception.UmsUnavailableException if nothing is cached and the UMS can't answer */
    TeamScope cached(String sub);

    /** @throws com.bnaqica.crmdemocoreservice.exception.UmsUnavailableException if the UMS can't answer */
    TeamScope live(String sub);

    /** Handles a message from {@code ums-user-events}: evicts the cached scope of the user it names. */
    void onUserEvent(String payload);
}

package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.dto.TeamFilter;
import java.util.UUID;

/**
 * Team-scope checks for the current user, for services that own team records. Reads use the cached scope; edits use
 * the live scope.
 */
public interface TeamAccessService {

    /** The teams the current user may list records from; pass to every list query. */
    TeamFilter readableTeams();

    /**
     * @throws com.bnaqica.crmdemocoreservice.exception.RecordNotFoundException if the user may not read the team's
     *     records; a Super Manager's read of another team is audited
     */
    void checkView(UUID teamId, String recordType, String recordId);

    /** @throws com.bnaqica.crmdemocoreservice.exception.TeamAccessDeniedException unless the user is in the team */
    void checkEdit(UUID teamId);

    /**
     * For changing an existing record: as {@link #checkEdit}, but a record the user can't even read is reported as not
     * found, so its existence doesn't leak.
     *
     * @throws com.bnaqica.crmdemocoreservice.exception.RecordNotFoundException if the user may not read it
     * @throws com.bnaqica.crmdemocoreservice.exception.TeamAccessDeniedException if they may read but not change it
     */
    void checkChange(UUID teamId, String recordType, String recordId);

    /** @throws com.bnaqica.crmdemocoreservice.exception.TeamAccessDeniedException unless the user may manage the team */
    void checkManage(UUID teamId);
}

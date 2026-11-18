package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.entity.PortalUser;
import java.util.UUID;

/** CRUD for {@code portal_user}, core-service's copy of portal users. */
public interface PortalUserService {

    /** Creates the copy, or updates its email; returns it. */
    PortalUser upsert(UUID id, String email);
}

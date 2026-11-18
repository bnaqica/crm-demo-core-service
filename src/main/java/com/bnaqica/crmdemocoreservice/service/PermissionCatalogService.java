package com.bnaqica.crmdemocoreservice.service;

/** Registers the permissions this service enforces with the UMS catalog. */
public interface PermissionCatalogService {

    /** Registers now; on failure logs it and tries again later, until it succeeds. Never throws. */
    void register();
}

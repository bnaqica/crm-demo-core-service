package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.dto.ApiAuditEntry;

public interface ApiAuditService {

    void record(ApiAuditEntry entry);
}

package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.dto.response.CapabilitiesResponse;

/** The resolved capabilities of the current user. */
public interface CapabilitiesService {

    CapabilitiesResponse current();
}

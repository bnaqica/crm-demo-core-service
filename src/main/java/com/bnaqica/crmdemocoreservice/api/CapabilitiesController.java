package com.bnaqica.crmdemocoreservice.api;

import com.bnaqica.crmdemocoreservice.config.AnyActiveInternalUser;
import com.bnaqica.crmdemocoreservice.dto.response.CapabilitiesResponse;
import com.bnaqica.crmdemocoreservice.service.CapabilitiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** What the current user may see and do in the CRM UI. */
@RestController
@RequiredArgsConstructor
public class CapabilitiesController {

    private final CapabilitiesService capabilities;

    @GetMapping("/me/capabilities")
    @AnyActiveInternalUser
    CapabilitiesResponse capabilities() {
        return capabilities.current();
    }
}

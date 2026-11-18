package com.bnaqica.crmdemocoreservice.listener;

import com.bnaqica.crmdemocoreservice.service.PermissionCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Registers this service's permissions with the UMS catalog once the application is up. */
@Component
@RequiredArgsConstructor
public class ApplicationReadyListener {

    private final PermissionCatalogService catalog;

    @EventListener(ApplicationReadyEvent.class)
    void onReady() {
        catalog.register();
    }
}

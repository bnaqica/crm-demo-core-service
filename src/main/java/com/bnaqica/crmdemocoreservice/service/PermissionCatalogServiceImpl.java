package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.client.UmsClient;
import com.bnaqica.crmdemocoreservice.config.CrmProperties;
import com.bnaqica.crmdemocoreservice.dto.CatalogRegistration;
import com.bnaqica.crmdemocoreservice.processors.DeclaredPermissions;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

/** A failed registration doesn't stop the service: it is logged and tried again every {@code retryInterval}. */
@Slf4j
@Service
public class PermissionCatalogServiceImpl implements PermissionCatalogService {

    private final UmsClient ums;
    private final DeclaredPermissions declared;
    private final TaskScheduler scheduler;
    private final Clock clock;
    private final String serviceName;
    private final Duration retryInterval;

    @Autowired
    public PermissionCatalogServiceImpl(
            UmsClient ums,
            DeclaredPermissions declared,
            TaskScheduler scheduler,
            Clock clock,
            CrmProperties properties) {
        this(
                ums,
                declared,
                scheduler,
                clock,
                properties.catalog().serviceName(),
                properties.catalog().retryInterval());
    }

    PermissionCatalogServiceImpl(
            UmsClient ums,
            DeclaredPermissions declared,
            TaskScheduler scheduler,
            Clock clock,
            String serviceName,
            Duration retryInterval) {
        this.ums = ums;
        this.declared = declared;
        this.scheduler = scheduler;
        this.clock = clock;
        this.serviceName = serviceName;
        this.retryInterval = retryInterval;
    }

    @Override
    public void register() {
        try {
            var permissions = declared.catalog();
            ums.registerCatalog(serviceName, new CatalogRegistration(permissions));
            log.info("Registered {} permissions with the UMS catalog as {}", permissions.size(), serviceName);
        } catch (RuntimeException ex) {
            log.error("Couldn't register permissions with the UMS catalog; retrying in {}", retryInterval, ex);
            scheduler.schedule(this::register, Instant.now(clock).plus(retryInterval));
        }
    }
}

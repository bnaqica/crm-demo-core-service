package com.bnaqica.crmdemocoreservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bnaqica.crmdemocoreservice.client.UmsClient;
import com.bnaqica.crmdemocoreservice.dto.CatalogRegistration;
import com.bnaqica.crmdemocoreservice.dto.CatalogRegistration.Permission;
import com.bnaqica.crmdemocoreservice.exception.UmsClientException;
import com.bnaqica.crmdemocoreservice.processors.DeclaredPermissions;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

class PermissionCatalogServiceImplTests {

    private static final List<Permission> PERMISSIONS = List.of(
            new Permission("submissions:read", "crm-internal", "Read portal submissions", false),
            new Permission("submissions:write", "crm-internal", "Decide portal submissions", false));

    private final UmsClient ums = mock(UmsClient.class);
    private final DeclaredPermissions declared = mock(DeclaredPermissions.class);
    private final TaskScheduler scheduler = mock(TaskScheduler.class);
    private final PermissionCatalogService service = new PermissionCatalogServiceImpl(
            ums, declared, scheduler, Clock.systemUTC(), "core-service", Duration.ofSeconds(30));

    @Test
    void registersEveryDeclaredPermissionUnderTheServiceName() {
        when(declared.catalog()).thenReturn(PERMISSIONS);

        service.register();

        verify(ums).registerCatalog("core-service", new CatalogRegistration(PERMISSIONS));
        verify(scheduler, never()).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void failedRegistrationIsRetriedLater() {
        when(declared.catalog()).thenReturn(PERMISSIONS);
        doThrow(new UmsClientException(503, "down")).doNothing().when(ums).registerCatalog(eq("core-service"), any());

        service.register();

        var retry = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(retry.capture(), any(Instant.class));
        retry.getValue().run();
        verify(ums, times(2)).registerCatalog("core-service", new CatalogRegistration(PERMISSIONS));
    }
}

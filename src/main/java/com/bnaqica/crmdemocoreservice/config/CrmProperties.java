package com.bnaqica.crmdemocoreservice.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.hibernate.validator.constraints.time.DurationMax;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Settings under {@code crm.*}.
 *
 * @param ums where the User Management Service is
 * @param scopeCache how long a user's team scope is cached for reads (at most 5 minutes)
 * @param catalog how this service registers its permissions with the UMS
 * @param outbox how often committed events are published
 * @param routing where submissions go when no routing rule matches
 * @param topics Kafka topic names
 * @param cors browser origins allowed to call the API (the CRM UI)
 */
@Validated
@ConfigurationProperties("crm")
public record CrmProperties(
        Ums ums,
        @DefaultValue ScopeCache scopeCache,
        @DefaultValue Catalog catalog,
        @DefaultValue Outbox outbox,
        Routing routing,
        Topics topics,
        Cors cors) {

    /** @param baseUrl the UMS address, for example {@code http://localhost:8083} */
    public record Ums(@NotBlank String baseUrl) {}

    public record ScopeCache(
            @DefaultValue("5m") @DurationMax(minutes = 5) Duration ttl) {}

    /**
     * @param serviceName the name the permissions are registered under
     * @param retryInterval how long to wait before trying a failed registration again
     */
    public record Catalog(
            @DefaultValue("core-service") String serviceName,
            @DefaultValue("30s") Duration retryInterval) {}

    public record Outbox(@DefaultValue("1s") Duration relayInterval) {}

    /** @param defaultTeamId the UMS team that gets submissions no rule matches */
    public record Routing(@NotNull UUID defaultTeamId) {}

    public record Topics(
            @NotBlank String userEvents,
            @NotBlank String auditEvents,
            @NotBlank String portalIntake,
            @NotBlank String submissionStatus) {}

    public record Cors(@NotEmpty List<String> allowedOrigins) {}
}

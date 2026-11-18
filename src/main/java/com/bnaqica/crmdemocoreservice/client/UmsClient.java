package com.bnaqica.crmdemocoreservice.client;

import com.bnaqica.crmdemocoreservice.dto.CatalogRegistration;
import com.bnaqica.crmdemocoreservice.dto.TeamScope;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

/**
 * The UMS internal API, called as the {@code core-service} client of {@code crm-internal} (scopes
 * {@code ums:team-scope} and {@code ums:catalog}); see {@code UmsClientConfig}.
 */
@HttpExchange(accept = "application/json")
public interface UmsClient {

    @GetExchange("/internal/users/{sub}/team-scope")
    TeamScope teamScope(@PathVariable String sub);

    /** Replaces the service's catalog entries with {@code registration}; idempotent. */
    @PutExchange(url = "/internal/catalog/{service}", contentType = "application/json")
    void registerCatalog(@PathVariable String service, @RequestBody CatalogRegistration registration);
}

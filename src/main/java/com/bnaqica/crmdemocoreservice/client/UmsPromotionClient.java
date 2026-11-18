package com.bnaqica.crmdemocoreservice.client;

import com.bnaqica.crmdemocoreservice.dto.PromotedUser;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * The UMS promote API, called as the {@code crm-promoter} client of {@code crm-internal} (scope {@code ums:promote}),
 * kept apart from {@link UmsClient} so only this call carries that scope; see {@code UmsClientConfig}.
 */
@HttpExchange(accept = "application/json")
public interface UmsPromotionClient {

    /** Replaces the external user's starter role with standard; idempotent. */
    @PostExchange("/internal/users/{id}/promote")
    PromotedUser promote(@PathVariable UUID id, @RequestHeader("X-Correlation-Id") String correlationId);
}

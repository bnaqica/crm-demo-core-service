package com.bnaqica.crmdemocoreservice.dto.response;

import java.util.UUID;
import lombok.Builder;

/** A routing rule; rules are tried by ascending {@code position}. */
@Builder
public record RoutingRuleResponse(UUID id, int position, String type, String region, UUID teamId) {}

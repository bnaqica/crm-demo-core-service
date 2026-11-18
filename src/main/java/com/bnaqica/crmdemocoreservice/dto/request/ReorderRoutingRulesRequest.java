package com.bnaqica.crmdemocoreservice.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** Every routing rule's id, in the new order. */
public record ReorderRoutingRulesRequest(@NotNull List<@NotNull UUID> ruleIds) {}

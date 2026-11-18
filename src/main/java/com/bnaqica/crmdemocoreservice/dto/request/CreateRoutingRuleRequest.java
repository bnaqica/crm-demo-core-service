package com.bnaqica.crmdemocoreservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** A rule sending submissions of {@code type} from {@code region} to the UMS team {@code teamId}. */
public record CreateRoutingRuleRequest(
        @NotBlank @Size(max = 100) String type,
        @NotBlank @Size(max = 100) String region,
        @NotNull UUID teamId) {}

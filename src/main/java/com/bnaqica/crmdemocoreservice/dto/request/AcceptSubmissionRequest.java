package com.bnaqica.crmdemocoreservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** @param version the submission version the user looked at; a later correction makes the decision a 409 */
public record AcceptSubmissionRequest(@NotNull @Min(1) Integer version) {}

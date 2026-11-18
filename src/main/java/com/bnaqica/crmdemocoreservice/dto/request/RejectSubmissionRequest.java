package com.bnaqica.crmdemocoreservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param reason shown to the submitter in the portal
 * @param version the submission version the user looked at; a later correction makes the decision a 409
 */
public record RejectSubmissionRequest(
        @NotBlank @Size(max = 500) String reason,
        @NotNull @Min(1) Integer version) {}

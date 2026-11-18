package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.dto.response.SubmissionResponse;
import java.util.UUID;

/**
 * Accepting and rejecting submissions, by members of the submission's team. Each decision publishes its status to the
 * portal.
 */
public interface SubmissionDecisionService {

    /**
     * @param version the version the user decided on
     * @throws com.bnaqica.crmdemocoreservice.exception.VersionMismatchException if the submission was corrected since
     */
    SubmissionResponse accept(UUID id, int version);

    /**
     * @param version the version the user decided on
     * @param reason shown to the submitter
     * @throws com.bnaqica.crmdemocoreservice.exception.VersionMismatchException if the submission was corrected since
     */
    SubmissionResponse reject(UUID id, int version, String reason);
}

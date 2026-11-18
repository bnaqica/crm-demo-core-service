package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.dto.response.PromotionResponse;
import java.util.UUID;

/** Promoting the portal user who sent a submission from starter to standard, through the UMS. */
public interface SubmitterPromotionService {

    /**
     * @throws com.bnaqica.crmdemocoreservice.exception.RecordNotFoundException if the submission is outside the
     *     caller's team scope; the UMS isn't called then
     * @throws com.bnaqica.crmdemocoreservice.exception.UmsRequestFailedException if the UMS refuses or fails
     */
    PromotionResponse promoteSubmitter(UUID submissionId);
}

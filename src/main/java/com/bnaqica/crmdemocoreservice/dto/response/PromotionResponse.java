package com.bnaqica.crmdemocoreservice.dto.response;

import java.util.UUID;

/**
 * The submitter after promotion.
 *
 * @param status the UMS account status after promotion, such as {@code ACTIVE}
 * @param tier always {@code STANDARD} once promoted
 */
public record PromotionResponse(UUID portalUserId, boolean promoted, String tier, String status) {}

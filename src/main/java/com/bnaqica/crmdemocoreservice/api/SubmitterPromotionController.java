package com.bnaqica.crmdemocoreservice.api;

import com.bnaqica.crmdemocoreservice.config.RequiresPermission;
import com.bnaqica.crmdemocoreservice.dto.response.PromotionResponse;
import com.bnaqica.crmdemocoreservice.service.SubmitterPromotionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Promotes a starter portal user who sent a submission to standard access. */
@RestController
@RequiredArgsConstructor
public class SubmitterPromotionController {

    private final SubmitterPromotionService promotions;

    /** Pass {@code X-Correlation-Id} to tie the UMS audit record to this request. */
    @PostMapping("/submissions/{id}/promote-submitter")
    @RequiresPermission(
            value = "portal-users:write",
            description = "Promote the starter portal users who sent the team's submissions to standard access")
    PromotionResponse promote(@PathVariable UUID id) {
        return promotions.promoteSubmitter(id);
    }
}

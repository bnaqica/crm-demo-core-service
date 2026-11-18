package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.client.UmsPromotionClient;
import com.bnaqica.crmdemocoreservice.dto.PromotedUser;
import com.bnaqica.crmdemocoreservice.dto.response.PromotionResponse;
import com.bnaqica.crmdemocoreservice.entity.Submission;
import com.bnaqica.crmdemocoreservice.exception.RecordNotFoundException;
import com.bnaqica.crmdemocoreservice.exception.UmsClientException;
import com.bnaqica.crmdemocoreservice.exception.UmsRequestFailedException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Members of the submission's team may promote its submitter. The UMS does the promotion (and audits it), so nothing
 * changes here; the caller's {@code X-Correlation-Id} is passed on, or one is made up.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitterPromotionServiceImpl implements SubmitterPromotionService {

    static final String CORRELATION_HEADER = "X-Correlation-Id";
    static final String STANDARD = "STANDARD";

    private final SubmissionService submissions;
    private final TeamAccessService teamAccess;
    private final UmsPromotionClient ums;

    @Override
    public PromotionResponse promoteSubmitter(UUID submissionId) {
        Submission submission = submissions
                .find(submissionId)
                .orElseThrow(() -> new RecordNotFoundException(SubmissionServiceImpl.RECORD_TYPE, submissionId));
        teamAccess.checkChange(submission.getTeamId(), SubmissionServiceImpl.RECORD_TYPE, submissionId.toString());
        UUID submitter = submission.getPortalUser().getId();
        String correlationId = correlationId();
        try {
            PromotedUser promoted = ums.promote(submitter, correlationId);
            log.info("Promoted portal user {} (correlation {})", submitter, correlationId);
            return new PromotionResponse(submitter, true, STANDARD, promoted == null ? null : promoted.status());
        } catch (UmsClientException ex) {
            throw new UmsRequestFailedException("The UMS refused to promote portal user " + submitter, ex);
        } catch (RestClientException | OAuth2AuthorizationException ex) {
            throw new UmsRequestFailedException("The UMS couldn't be reached to promote " + submitter, ex);
        }
    }

    private static String correlationId() {
        return CurrentUser.request()
                .map(request -> request.getHeader(CORRELATION_HEADER))
                .filter(header -> !header.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
    }
}

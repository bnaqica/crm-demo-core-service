package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.dto.response.SubmissionResponse;
import com.bnaqica.crmdemocoreservice.entity.Submission;
import com.bnaqica.crmdemocoreservice.entity.SubmissionStatus;
import com.bnaqica.crmdemocoreservice.exception.InvalidStatusTransitionException;
import com.bnaqica.crmdemocoreservice.exception.RecordNotFoundException;
import com.bnaqica.crmdemocoreservice.exception.VersionMismatchException;
import com.bnaqica.crmdemocoreservice.publisher.SubmissionStatusPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Only a RECEIVED submission can be decided, once, and only at the version the user looked at (the portal may have sent
 * a correction since). Two people deciding at the same time can't both win: the
 * submission's row version makes the second commit fail with 409.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SubmissionDecisionServiceImpl implements SubmissionDecisionService {

    private final SubmissionService submissions;
    private final TeamAccessService teamAccess;
    private final SubmissionStatusPublisher statusPublisher;
    private final Clock clock;

    @Override
    public SubmissionResponse accept(UUID id, int version) {
        return decide(id, version, SubmissionStatus.ACCEPTED, null);
    }

    @Override
    public SubmissionResponse reject(UUID id, int version, String reason) {
        return decide(id, version, SubmissionStatus.REJECTED, reason);
    }

    private SubmissionResponse decide(UUID id, int version, SubmissionStatus decision, String reason) {
        Submission submission = submissions
                .find(id)
                .orElseThrow(() -> new RecordNotFoundException(SubmissionServiceImpl.RECORD_TYPE, id));
        teamAccess.checkChange(submission.getTeamId(), SubmissionServiceImpl.RECORD_TYPE, id.toString());
        if (submission.getVersion() != version) {
            throw new VersionMismatchException("Submission " + id + " is now at version " + submission.getVersion()
                    + "; reload it before deciding");
        }
        if (submission.getStatus() != SubmissionStatus.RECEIVED) {
            throw new InvalidStatusTransitionException("Submission " + id + " is already " + submission.getStatus());
        }
        submission.setStatus(decision);
        submission.setRejectionReason(reason);
        submission.setDecidedAt(Instant.now(clock));
        submission.setDecidedBy(CurrentUser.sub());
        Submission saved = submissions.save(submission);
        statusPublisher.decided(saved);
        log.info("Submission {} {} by {}", id, decision, saved.getDecidedBy());
        return SubmissionMapper.toResponse(saved);
    }
}

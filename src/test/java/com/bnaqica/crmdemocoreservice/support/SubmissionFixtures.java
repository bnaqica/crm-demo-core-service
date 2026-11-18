package com.bnaqica.crmdemocoreservice.support;

import com.bnaqica.crmdemocoreservice.entity.PortalUser;
import com.bnaqica.crmdemocoreservice.entity.Submission;
import com.bnaqica.crmdemocoreservice.entity.SubmissionDocument;
import com.bnaqica.crmdemocoreservice.entity.SubmissionStatus;
import com.bnaqica.crmdemocoreservice.service.PortalUserService;
import com.bnaqica.crmdemocoreservice.service.SubmissionService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/** Stores submissions directly, for API tests that don't need to go through Kafka. */
@RequiredArgsConstructor
public class SubmissionFixtures {

    private final PortalUserService portalUsers;
    private final SubmissionService submissions;

    public Submission submission(UUID team, String type, SubmissionStatus status, Instant receivedAt) {
        PortalUser submitter = portalUsers.upsert(UUID.randomUUID(), "submitter@acme.example");
        return submissions.save(Submission.builder()
                .id(UUID.randomUUID())
                .portalUser(submitter)
                .version(1)
                .type(type)
                .region("EU")
                .teamId(team)
                .status(status)
                .receivedAt(receivedAt)
                .documents(new ArrayList<>(List.of(new SubmissionDocument(
                        UUID.randomUUID(), "u/1/invoice.pdf", "application/pdf", 1234, "a".repeat(64)))))
                .build());
    }
}

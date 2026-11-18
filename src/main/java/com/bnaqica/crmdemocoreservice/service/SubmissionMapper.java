package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.dto.response.SubmissionResponse;
import com.bnaqica.crmdemocoreservice.entity.Submission;

/** Maps submissions to what the API returns. Call inside a transaction: the submitter and documents load lazily. */
final class SubmissionMapper {

    private SubmissionMapper() {}

    static SubmissionResponse toResponse(Submission submission) {
        return SubmissionResponse.builder()
                .id(submission.getId())
                .version(submission.getVersion())
                .type(submission.getType())
                .region(submission.getRegion())
                .status(submission.getStatus().name())
                .teamId(submission.getTeamId())
                .portalUserId(submission.getPortalUser().getId())
                .submitterEmail(submission.getPortalUser().getEmail())
                .receivedAt(submission.getReceivedAt())
                .decidedAt(submission.getDecidedAt())
                .rejectionReason(submission.getRejectionReason())
                .documents(submission.getDocuments().stream()
                        .map(document -> new SubmissionResponse.Document(
                                document.getDocumentId(),
                                document.getContentType(),
                                document.getSize(),
                                document.getSha256()))
                        .toList())
                .build();
    }
}

package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.dto.SubmissionSearch;
import com.bnaqica.crmdemocoreservice.dto.response.SubmissionPageResponse;
import com.bnaqica.crmdemocoreservice.dto.response.SubmissionResponse;
import com.bnaqica.crmdemocoreservice.entity.Submission;
import java.util.Optional;
import java.util.UUID;

/**
 * CRUD for {@code submission}. The {@code search} and {@code view} methods apply the current user's team scope; the
 * entity methods are for other services and apply none.
 */
public interface SubmissionService {

    /** Submissions in the current user's team scope matching {@code search}, newest first. */
    SubmissionPageResponse search(SubmissionSearch search, int page, int size);

    /**
     * @throws com.bnaqica.crmdemocoreservice.exception.RecordNotFoundException if it doesn't exist or is outside the
     *     current user's team scope
     */
    SubmissionResponse view(UUID id);

    Optional<Submission> find(UUID id);

    Submission save(Submission submission);
}

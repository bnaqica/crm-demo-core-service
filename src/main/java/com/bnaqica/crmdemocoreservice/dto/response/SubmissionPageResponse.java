package com.bnaqica.crmdemocoreservice.dto.response;

import java.util.List;

/** One page of submissions, newest first. {@code page} counts from 0. */
public record SubmissionPageResponse(
        List<SubmissionResponse> content, int page, int size, long totalElements, int totalPages) {}

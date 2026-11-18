package com.bnaqica.crmdemocoreservice.dto;

import com.bnaqica.crmdemocoreservice.entity.SubmissionStatus;
import java.time.Instant;

/**
 * Optional filters for listing submissions; null means any.
 *
 * @param receivedFrom inclusive
 * @param receivedTo exclusive
 */
public record SubmissionSearch(SubmissionStatus status, String type, Instant receivedFrom, Instant receivedTo) {}

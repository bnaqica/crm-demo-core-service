package com.bnaqica.crmdemocoreservice.api;

import com.bnaqica.crmdemocoreservice.config.RequiresPermission;
import com.bnaqica.crmdemocoreservice.dto.SubmissionSearch;
import com.bnaqica.crmdemocoreservice.dto.request.AcceptSubmissionRequest;
import com.bnaqica.crmdemocoreservice.dto.request.RejectSubmissionRequest;
import com.bnaqica.crmdemocoreservice.dto.response.SubmissionPageResponse;
import com.bnaqica.crmdemocoreservice.dto.response.SubmissionResponse;
import com.bnaqica.crmdemocoreservice.entity.SubmissionStatus;
import com.bnaqica.crmdemocoreservice.service.SubmissionDecisionService;
import com.bnaqica.crmdemocoreservice.service.SubmissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Portal submissions routed to the caller's teams. */
@RestController
@RequestMapping("/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    static final String READ = "submissions:read";
    static final String READ_DESCRIPTION = "See portal submissions routed to the user's teams";
    static final String WRITE = "submissions:write";
    static final String WRITE_DESCRIPTION = "Accept or reject portal submissions of the user's teams";

    private final SubmissionService submissions;
    private final SubmissionDecisionService decisions;

    /** Filters are optional; {@code receivedFrom} is inclusive and {@code receivedTo} exclusive (ISO-8601 instants). */
    @GetMapping
    @RequiresPermission(value = READ, description = READ_DESCRIPTION)
    SubmissionPageResponse list(
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant receivedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant receivedTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return submissions.search(new SubmissionSearch(status, type, receivedFrom, receivedTo), page, size);
    }

    @GetMapping("/{id}")
    @RequiresPermission(value = READ, description = READ_DESCRIPTION)
    SubmissionResponse view(@PathVariable UUID id) {
        return submissions.view(id);
    }

    @PostMapping("/{id}/accept")
    @RequiresPermission(value = WRITE, description = WRITE_DESCRIPTION)
    SubmissionResponse accept(@PathVariable UUID id, @Valid @RequestBody AcceptSubmissionRequest request) {
        return decisions.accept(id, request.version());
    }

    /** The reason is shown to the submitter. Both decisions name the version the user looked at. */
    @PostMapping("/{id}/reject")
    @RequiresPermission(value = WRITE, description = WRITE_DESCRIPTION)
    SubmissionResponse reject(@PathVariable UUID id, @Valid @RequestBody RejectSubmissionRequest request) {
        return decisions.reject(id, request.version(), request.reason());
    }
}

package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.dto.SubmissionSearch;
import com.bnaqica.crmdemocoreservice.dto.TeamFilter;
import com.bnaqica.crmdemocoreservice.dto.response.SubmissionPageResponse;
import com.bnaqica.crmdemocoreservice.dto.response.SubmissionResponse;
import com.bnaqica.crmdemocoreservice.entity.Submission;
import com.bnaqica.crmdemocoreservice.exception.RecordNotFoundException;
import com.bnaqica.crmdemocoreservice.repository.SubmissionRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SubmissionServiceImpl implements SubmissionService {

    static final String RECORD_TYPE = "SUBMISSION";

    private final SubmissionRepository repository;
    private final TeamAccessService teamAccess;

    @Override
    @Transactional(readOnly = true)
    public SubmissionPageResponse search(SubmissionSearch search, int page, int size) {
        TeamFilter teams = teamAccess.readableTeams();
        if (!teams.allTeams() && teams.teams().isEmpty()) {
            return new SubmissionPageResponse(List.of(), page, size, 0, 0);
        }
        Page<Submission> found = repository.findAll(
                matching(search, teams),
                PageRequest.of(
                        page, size, Sort.by(Sort.Direction.DESC, "receivedAt").and(Sort.by("id"))));
        return new SubmissionPageResponse(
                found.map(SubmissionMapper::toResponse).getContent(),
                page,
                size,
                found.getTotalElements(),
                found.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponse view(UUID id) {
        Submission submission = repository.findById(id).orElseThrow(() -> new RecordNotFoundException(RECORD_TYPE, id));
        teamAccess.checkView(submission.getTeamId(), RECORD_TYPE, id.toString());
        return SubmissionMapper.toResponse(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Submission> find(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Submission save(Submission submission) {
        return repository.save(submission);
    }

    private static Specification<Submission> matching(SubmissionSearch search, TeamFilter teams) {
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (!teams.allTeams()) {
                where.add(root.get("teamId").in(teams.teams()));
            }
            if (search.status() != null) {
                where.add(cb.equal(root.get("status"), search.status()));
            }
            if (search.type() != null) {
                where.add(cb.equal(root.get("type"), search.type()));
            }
            if (search.receivedFrom() != null) {
                where.add(cb.greaterThanOrEqualTo(root.get("receivedAt"), search.receivedFrom()));
            }
            if (search.receivedTo() != null) {
                where.add(cb.lessThan(root.get("receivedAt"), search.receivedTo()));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }
}

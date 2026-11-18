package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.dto.request.CreateRoutingRuleRequest;
import com.bnaqica.crmdemocoreservice.dto.response.RoutingRuleResponse;
import com.bnaqica.crmdemocoreservice.entity.RoutingRule;
import com.bnaqica.crmdemocoreservice.exception.InvalidRequestException;
import com.bnaqica.crmdemocoreservice.exception.RecordNotFoundException;
import com.bnaqica.crmdemocoreservice.repository.RoutingRuleRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoutingRuleServiceImpl implements RoutingRuleService {

    static final String RECORD_TYPE = "ROUTING_RULE";

    private final RoutingRuleRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<RoutingRuleResponse> list() {
        return repository.findAllByOrderByPositionAsc().stream()
                .map(RoutingRuleServiceImpl::toResponse)
                .toList();
    }

    @Override
    public RoutingRuleResponse create(CreateRoutingRuleRequest request) {
        int last = repository.findAllByOrderByPositionAsc().stream()
                .mapToInt(RoutingRule::getPosition)
                .max()
                .orElse(0);
        RoutingRule rule = repository.save(RoutingRule.builder()
                .id(UUID.randomUUID())
                .position(last + 1)
                .type(request.type())
                .region(request.region())
                .teamId(request.teamId())
                .build());
        log.info("Created routing rule {} at position {}", rule.getId(), rule.getPosition());
        return toResponse(rule);
    }

    @Override
    public void delete(UUID id) {
        RoutingRule rule = repository.findById(id).orElseThrow(() -> new RecordNotFoundException(RECORD_TYPE, id));
        repository.delete(rule);
        log.info("Deleted routing rule {}", id);
    }

    @Override
    public List<RoutingRuleResponse> reorder(List<UUID> ruleIds) {
        Map<UUID, RoutingRule> rules =
                repository.findAll().stream().collect(Collectors.toMap(RoutingRule::getId, Function.identity()));
        if (ruleIds.size() != rules.size() || !new HashSet<>(ruleIds).equals(rules.keySet())) {
            throw new InvalidRequestException("ruleIds must list every routing rule exactly once");
        }
        for (int i = 0; i < ruleIds.size(); i++) {
            rules.get(ruleIds.get(i)).setPosition(i + 1);
        }
        repository.flush();
        log.info("Reordered {} routing rules", ruleIds.size());
        return list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> teamFor(String type, String region) {
        return repository.findAllByOrderByPositionAsc().stream()
                .filter(rule -> rule.getType().equals(type) && rule.getRegion().equals(region))
                .map(RoutingRule::getTeamId)
                .findFirst();
    }

    private static RoutingRuleResponse toResponse(RoutingRule rule) {
        return RoutingRuleResponse.builder()
                .id(rule.getId())
                .position(rule.getPosition())
                .type(rule.getType())
                .region(rule.getRegion())
                .teamId(rule.getTeamId())
                .build();
    }
}

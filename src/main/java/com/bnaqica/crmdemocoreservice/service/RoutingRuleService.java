package com.bnaqica.crmdemocoreservice.service;

import com.bnaqica.crmdemocoreservice.dto.request.CreateRoutingRuleRequest;
import com.bnaqica.crmdemocoreservice.dto.response.RoutingRuleResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** CRUD for {@code routing_rule}. Rules are kept in order; the first one matching a submission wins. */
public interface RoutingRuleService {

    /** All rules, first one first. */
    List<RoutingRuleResponse> list();

    /** Adds a rule after the existing ones. */
    RoutingRuleResponse create(CreateRoutingRuleRequest request);

    /** @throws com.bnaqica.crmdemocoreservice.exception.RecordNotFoundException for an unknown id */
    void delete(UUID id);

    /**
     * Puts the rules in the given order.
     *
     * @throws com.bnaqica.crmdemocoreservice.exception.InvalidRequestException unless {@code ruleIds} lists every rule
     *     exactly once
     */
    List<RoutingRuleResponse> reorder(List<UUID> ruleIds);

    /** The team of the first rule matching {@code type} and {@code region}; empty if none matches. */
    Optional<UUID> teamFor(String type, String region);
}

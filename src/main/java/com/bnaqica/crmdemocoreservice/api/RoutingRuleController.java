package com.bnaqica.crmdemocoreservice.api;

import com.bnaqica.crmdemocoreservice.config.RequiresPermission;
import com.bnaqica.crmdemocoreservice.dto.request.CreateRoutingRuleRequest;
import com.bnaqica.crmdemocoreservice.dto.request.ReorderRoutingRulesRequest;
import com.bnaqica.crmdemocoreservice.dto.response.RoutingRuleResponse;
import com.bnaqica.crmdemocoreservice.service.RoutingRuleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The rules that route new portal submissions to teams. Rules apply to all teams, so they aren't team-scoped. */
@RestController
@RequestMapping("/routing-rules")
@RequiredArgsConstructor
public class RoutingRuleController {

    static final String READ = "routing-rules:read";
    static final String READ_DESCRIPTION = "See the rules that route portal submissions to teams";
    static final String WRITE = "routing-rules:write";
    static final String WRITE_DESCRIPTION = "Add, reorder and delete the rules that route portal submissions to teams";

    private final RoutingRuleService routingRules;

    @GetMapping
    @RequiresPermission(value = READ, description = READ_DESCRIPTION)
    List<RoutingRuleResponse> list() {
        return routingRules.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(value = WRITE, description = WRITE_DESCRIPTION)
    RoutingRuleResponse create(@Valid @RequestBody CreateRoutingRuleRequest request) {
        return routingRules.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission(value = WRITE, description = WRITE_DESCRIPTION)
    void delete(@PathVariable UUID id) {
        routingRules.delete(id);
    }

    @PutMapping("/order")
    @RequiresPermission(value = WRITE, description = WRITE_DESCRIPTION)
    List<RoutingRuleResponse> reorder(@Valid @RequestBody ReorderRoutingRulesRequest request) {
        return routingRules.reorder(request.ruleIds());
    }
}

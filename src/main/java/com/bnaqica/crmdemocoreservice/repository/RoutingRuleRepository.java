package com.bnaqica.crmdemocoreservice.repository;

import com.bnaqica.crmdemocoreservice.entity.RoutingRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutingRuleRepository extends JpaRepository<RoutingRule, UUID> {

    List<RoutingRule> findAllByOrderByPositionAsc();
}

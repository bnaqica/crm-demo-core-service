package com.bnaqica.crmdemocoreservice.repository;

import com.bnaqica.crmdemocoreservice.entity.ProcessedEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {}

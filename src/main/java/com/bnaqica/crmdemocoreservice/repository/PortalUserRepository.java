package com.bnaqica.crmdemocoreservice.repository;

import com.bnaqica.crmdemocoreservice.entity.PortalUser;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortalUserRepository extends JpaRepository<PortalUser, UUID> {}

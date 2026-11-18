package com.bnaqica.crmdemocoreservice.repository;

import com.bnaqica.crmdemocoreservice.entity.Submission;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SubmissionRepository extends JpaRepository<Submission, UUID>, JpaSpecificationExecutor<Submission> {}

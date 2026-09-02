package com.jobreadiness.copilot.readiness.repository;

import com.jobreadiness.copilot.readiness.entity.ReadinessAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ReadinessAnalysisRepository extends JpaRepository<ReadinessAnalysis, UUID> {
    Optional<ReadinessAnalysis> findFirstByJobIdAndUserIdOrderByCreatedAtDesc(UUID jobId, UUID userId);
}

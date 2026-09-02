package com.jobreadiness.copilot.readiness.repository;

import com.jobreadiness.copilot.readiness.entity.SkillAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SkillAssessmentRepository extends JpaRepository<SkillAssessment, UUID> {
    List<SkillAssessment> findByReadinessAnalysisId(UUID analysisId);
}

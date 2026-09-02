package com.jobreadiness.copilot.proof.repository;

import com.jobreadiness.copilot.proof.entity.ProofSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProofSubmissionRepository extends JpaRepository<ProofSubmission, UUID> {
    List<ProofSubmission> findByLearningMissionId(UUID missionId);
}

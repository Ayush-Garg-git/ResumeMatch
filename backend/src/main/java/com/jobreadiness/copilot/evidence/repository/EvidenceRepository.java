package com.jobreadiness.copilot.evidence.repository;

import com.jobreadiness.copilot.evidence.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {
    List<Evidence> findByUserId(UUID userId);
    List<Evidence> findByUserIdAndSkillId(UUID userId, UUID skillId);
    Optional<Evidence> findByIdAndUserId(UUID id, UUID userId);
}

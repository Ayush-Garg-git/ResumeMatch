package com.jobreadiness.copilot.roadmap.repository;

import com.jobreadiness.copilot.roadmap.entity.LearningMission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningMissionRepository extends JpaRepository<LearningMission, UUID> {
    List<LearningMission> findByUserId(UUID userId);
    Optional<LearningMission> findByUserIdAndSkillId(UUID userId, UUID skillId);
    Optional<LearningMission> findByIdAndUserId(UUID id, UUID userId);
}

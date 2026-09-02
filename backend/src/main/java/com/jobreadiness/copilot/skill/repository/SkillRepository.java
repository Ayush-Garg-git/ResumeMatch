package com.jobreadiness.copilot.skill.repository;

import com.jobreadiness.copilot.skill.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
    Optional<Skill> findByNameIgnoreCase(String name);
}

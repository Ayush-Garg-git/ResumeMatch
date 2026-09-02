package com.jobreadiness.copilot.resume.repository;

import com.jobreadiness.copilot.resume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {
    List<Resume> findByUserId(UUID userId);
    Optional<Resume> findByIdAndUserId(UUID id, UUID userId);
}

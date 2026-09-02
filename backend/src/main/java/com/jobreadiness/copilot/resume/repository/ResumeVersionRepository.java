package com.jobreadiness.copilot.resume.repository;

import com.jobreadiness.copilot.resume.entity.ResumeVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, UUID> {
    List<ResumeVersion> findByResumeIdAndUserId(UUID resumeId, UUID userId);
    Optional<ResumeVersion> findByIdAndUserId(UUID id, UUID userId);
}

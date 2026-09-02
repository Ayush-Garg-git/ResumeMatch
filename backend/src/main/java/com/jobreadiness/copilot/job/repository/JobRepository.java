package com.jobreadiness.copilot.job.repository;

import com.jobreadiness.copilot.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findByUserId(UUID userId);
    Optional<Job> findByIdAndUserId(UUID id, UUID userId);
}

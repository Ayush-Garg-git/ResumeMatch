package com.jobreadiness.copilot.job.repository;

import com.jobreadiness.copilot.job.entity.JobRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface JobRequirementRepository extends JpaRepository<JobRequirement, UUID> {
    List<JobRequirement> findByJobId(UUID jobId);
}

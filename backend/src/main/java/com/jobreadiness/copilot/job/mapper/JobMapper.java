package com.jobreadiness.copilot.job.mapper;

import com.jobreadiness.copilot.job.dto.JobDto;
import com.jobreadiness.copilot.job.dto.JobRequirementDto;
import com.jobreadiness.copilot.job.entity.Job;
import com.jobreadiness.copilot.job.entity.JobRequirement;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class JobMapper {

    public JobDto toDto(Job job) {
        if (job == null) return null;

        return JobDto.builder()
                .id(job.getId())
                .title(job.getTitle())
                .company(job.getCompany())
                .description(job.getDescription())
                .rawJdText(job.getRawJdText())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .requirements(job.getRequirements().stream().map(this::toDto).collect(Collectors.toList()))
                .build();
    }

    public JobRequirementDto toDto(JobRequirement req) {
        if (req == null) return null;

        return JobRequirementDto.builder()
                .id(req.getId())
                .type(req.getType())
                .description(req.getDescription())
                .skillId(req.getSkill() != null ? req.getSkill().getId() : null)
                .skillName(req.getSkill() != null ? req.getSkill().getName() : null)
                .build();
    }
}

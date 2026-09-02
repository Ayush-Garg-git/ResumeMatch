package com.jobreadiness.copilot.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequirementDto {
    private UUID id;
    private String type;
    private String description;
    private UUID skillId;
    private String skillName;
}

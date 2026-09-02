package com.jobreadiness.copilot.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDto {
    private UUID id;
    private String title;
    private String company;
    private String description;
    private String rawJdText;
    private String status;
    private List<JobRequirementDto> requirements;
    private LocalDateTime createdAt;
}

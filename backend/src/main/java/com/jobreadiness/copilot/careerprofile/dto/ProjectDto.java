package com.jobreadiness.copilot.careerprofile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    private UUID id;
    private String title;
    private String description;
    private String role;
    private String startDate;
    private String endDate;
    private String url;
}

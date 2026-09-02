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
public class ExperienceDto {
    private UUID id;
    private String company;
    private String role;
    private String description;
    private String startDate;
    private String endDate;
}

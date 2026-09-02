package com.jobreadiness.copilot.careerprofile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationDto {
    private UUID id;
    private String school;
    private String degree;
    private String fieldOfStudy;
    private String startDate;
    private String endDate;
    private BigDecimal gpa;
}

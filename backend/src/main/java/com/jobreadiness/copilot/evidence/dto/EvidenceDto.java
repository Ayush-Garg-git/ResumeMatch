package com.jobreadiness.copilot.evidence.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class EvidenceDto {
    private String skillName;
    private String title;
    private String description;
    private String evidenceType; // PROJECT, INTERNSHIP, COURSEWORK, CERTIFICATION, GITHUB, PORTFOLIO, STUDENT_RESPONSE, OTHER
    private String sourceType; // PROJECT, EXPERIENCE, CERTIFICATION, NONE
    private UUID sourceId;
    private String confidence; // STRONG, MEDIUM, WEAK
    private Boolean studentConfirmed;
}

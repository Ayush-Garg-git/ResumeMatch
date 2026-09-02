package com.jobreadiness.copilot.careerprofile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerProfileDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private String summary;
    private String targetRoles;
    private String githubUrl;
    private String linkedinUrl;
    private String portfolioUrl;
    private List<EducationDto> educations;
    private List<ProjectDto> projects;
    private List<ExperienceDto> experiences;
    private List<CertificationDto> certifications;
    private List<ProfileSkillDto> skills;
}

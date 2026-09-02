package com.jobreadiness.copilot.careerprofile.mapper;

import com.jobreadiness.copilot.careerprofile.dto.*;
import com.jobreadiness.copilot.careerprofile.entity.*;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class CareerProfileMapper {

    public CareerProfileDto toDto(CareerProfile profile) {
        if (profile == null) return null;

        return CareerProfileDto.builder()
                .id(profile.getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .summary(profile.getSummary())
                .targetRoles(profile.getTargetRoles())
                .githubUrl(profile.getGithubUrl())
                .linkedinUrl(profile.getLinkedinUrl())
                .portfolioUrl(profile.getPortfolioUrl())
                .educations(profile.getEducations().stream().map(this::toDto).collect(Collectors.toList()))
                .projects(profile.getProjects().stream().map(this::toDto).collect(Collectors.toList()))
                .experiences(profile.getExperiences().stream().map(this::toDto).collect(Collectors.toList()))
                .certifications(profile.getCertifications().stream().map(this::toDto).collect(Collectors.toList()))
                .skills(profile.getSkills().stream().map(this::toDto).collect(Collectors.toList()))
                .build();
    }

    public EducationDto toDto(Education education) {
        if (education == null) return null;
        return EducationDto.builder()
                .id(education.getId())
                .school(education.getSchool())
                .degree(education.getDegree())
                .fieldOfStudy(education.getFieldOfStudy())
                .startDate(education.getStartDate())
                .endDate(education.getEndDate())
                .gpa(education.getGpa())
                .build();
    }

    public ProjectDto toDto(Project project) {
        if (project == null) return null;
        return ProjectDto.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .role(project.getRole())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .url(project.getUrl())
                .build();
    }

    public ExperienceDto toDto(Experience experience) {
        if (experience == null) return null;
        return ExperienceDto.builder()
                .id(experience.getId())
                .company(experience.getCompany())
                .role(experience.getRole())
                .description(experience.getDescription())
                .startDate(experience.getStartDate())
                .endDate(experience.getEndDate())
                .build();
    }

    public CertificationDto toDto(Certification certification) {
        if (certification == null) return null;
        return CertificationDto.builder()
                .id(certification.getId())
                .name(certification.getName())
                .issuingOrganization(certification.getIssuingOrganization())
                .issueDate(certification.getIssueDate())
                .expirationDate(certification.getExpirationDate())
                .credentialUrl(certification.getCredentialUrl())
                .build();
    }

    public ProfileSkillDto toDto(ProfileSkill profileSkill) {
        if (profileSkill == null) return null;
        return ProfileSkillDto.builder()
                .skillId(profileSkill.getSkill().getId())
                .name(profileSkill.getSkill().getName())
                .category(profileSkill.getSkill().getCategory())
                .selfAssessedLevel(profileSkill.getSelfAssessedLevel())
                .build();
    }
}

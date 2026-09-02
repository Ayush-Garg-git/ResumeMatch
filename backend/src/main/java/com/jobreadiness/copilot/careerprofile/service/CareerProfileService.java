package com.jobreadiness.copilot.careerprofile.service;

import com.jobreadiness.copilot.careerprofile.dto.CareerProfileDto;
import com.jobreadiness.copilot.careerprofile.entity.*;
import com.jobreadiness.copilot.careerprofile.mapper.CareerProfileMapper;
import com.jobreadiness.copilot.careerprofile.repository.CareerProfileRepository;
import com.jobreadiness.copilot.skill.entity.Skill;
import com.jobreadiness.copilot.skill.repository.SkillRepository;
import com.jobreadiness.copilot.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class CareerProfileService {

    private final CareerProfileRepository profileRepository;
    private final SkillRepository skillRepository;
    private final CareerProfileMapper mapper;

    public CareerProfileService(
            CareerProfileRepository profileRepository,
            SkillRepository skillRepository,
            CareerProfileMapper mapper) {
        this.profileRepository = profileRepository;
        this.skillRepository = skillRepository;
        this.mapper = mapper;
    }

    @Transactional
    public CareerProfile getOrCreateProfileEntity(User user) {
        return profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    CareerProfile profile = CareerProfile.builder()
                            .user(user)
                            .educations(new ArrayList<>())
                            .projects(new ArrayList<>())
                            .experiences(new ArrayList<>())
                            .certifications(new ArrayList<>())
                            .skills(new ArrayList<>())
                            .build();
                    return profileRepository.save(profile);
                });
    }

    @Transactional(readOnly = true)
    public CareerProfileDto getProfile(User user) {
        CareerProfile profile = getOrCreateProfileEntity(user);
        return mapper.toDto(profile);
    }

    @Transactional
    public CareerProfileDto updateProfile(User user, CareerProfileDto dto) {
        CareerProfile profile = getOrCreateProfileEntity(user);

        profile.setFirstName(dto.getFirstName());
        profile.setLastName(dto.getLastName());
        profile.setSummary(dto.getSummary());
        profile.setTargetRoles(dto.getTargetRoles());
        profile.setGithubUrl(dto.getGithubUrl());
        profile.setLinkedinUrl(dto.getLinkedinUrl());
        profile.setPortfolioUrl(dto.getPortfolioUrl());

        // Sync Educations
        profile.getEducations().clear();
        if (dto.getEducations() != null) {
            profile.getEducations().addAll(dto.getEducations().stream().map(e ->
                Education.builder()
                        .profile(profile)
                        .school(e.getSchool())
                        .degree(e.getDegree())
                        .fieldOfStudy(e.getFieldOfStudy())
                        .startDate(e.getStartDate())
                        .endDate(e.getEndDate())
                        .gpa(e.getGpa())
                        .build()
            ).collect(Collectors.toList()));
        }

        // Sync Projects
        profile.getProjects().clear();
        if (dto.getProjects() != null) {
            profile.getProjects().addAll(dto.getProjects().stream().map(p ->
                Project.builder()
                        .profile(profile)
                        .title(p.getTitle())
                        .description(p.getDescription())
                        .role(p.getRole())
                        .startDate(p.getStartDate())
                        .endDate(p.getEndDate())
                        .url(p.getUrl())
                        .build()
            ).collect(Collectors.toList()));
        }

        // Sync Experiences
        profile.getExperiences().clear();
        if (dto.getExperiences() != null) {
            profile.getExperiences().addAll(dto.getExperiences().stream().map(exp ->
                Experience.builder()
                        .profile(profile)
                        .company(exp.getCompany())
                        .role(exp.getRole())
                        .description(exp.getDescription())
                        .startDate(exp.getStartDate())
                        .endDate(exp.getEndDate())
                        .build()
            ).collect(Collectors.toList()));
        }

        // Sync Certifications
        profile.getCertifications().clear();
        if (dto.getCertifications() != null) {
            profile.getCertifications().addAll(dto.getCertifications().stream().map(c ->
                Certification.builder()
                        .profile(profile)
                        .name(c.getName())
                        .issuingOrganization(c.getIssuingOrganization())
                        .issueDate(c.getIssueDate())
                        .expirationDate(c.getExpirationDate())
                        .credentialUrl(c.getCredentialUrl())
                        .build()
            ).collect(Collectors.toList()));
        }

        // Sync Skills
        profile.getSkills().clear();
        if (dto.getSkills() != null) {
            for (var skillDto : dto.getSkills()) {
                Skill skill = skillRepository.findByNameIgnoreCase(skillDto.getName())
                        .orElseGet(() -> {
                            Skill newSkill = Skill.builder()
                                    .name(skillDto.getName())
                                    .category(skillDto.getCategory())
                                    .build();
                            return skillRepository.save(newSkill);
                        });

                ProfileSkill profileSkill = ProfileSkill.builder()
                        .id(new ProfileSkill.ProfileSkillId(profile.getId(), skill.getId()))
                        .profile(profile)
                        .skill(skill)
                        .selfAssessedLevel(skillDto.getSelfAssessedLevel())
                        .build();

                profile.getSkills().add(profileSkill);
            }
        }

        CareerProfile updatedProfile = profileRepository.save(profile);
        return mapper.toDto(updatedProfile);
    }
}

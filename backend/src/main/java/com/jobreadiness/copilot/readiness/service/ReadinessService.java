package com.jobreadiness.copilot.readiness.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobreadiness.copilot.ai.service.AIService;
import com.jobreadiness.copilot.careerprofile.entity.CareerProfile;
import com.jobreadiness.copilot.careerprofile.repository.CareerProfileRepository;
import com.jobreadiness.copilot.common.exception.ResourceNotFoundException;
import com.jobreadiness.copilot.job.entity.Job;
import com.jobreadiness.copilot.job.repository.JobRepository;
import com.jobreadiness.copilot.readiness.entity.ReadinessAnalysis;
import com.jobreadiness.copilot.readiness.entity.SkillAssessment;
import com.jobreadiness.copilot.readiness.repository.ReadinessAnalysisRepository;
import com.jobreadiness.copilot.readiness.repository.SkillAssessmentRepository;
import com.jobreadiness.copilot.skill.entity.Skill;
import com.jobreadiness.copilot.skill.repository.SkillRepository;
import com.jobreadiness.copilot.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.UUID;

@Service
public class ReadinessService {

    private final ReadinessAnalysisRepository analysisRepository;
    private final SkillAssessmentRepository assessmentRepository;
    private final CareerProfileRepository profileRepository;
    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    public ReadinessService(
            ReadinessAnalysisRepository analysisRepository,
            SkillAssessmentRepository assessmentRepository,
            CareerProfileRepository profileRepository,
            JobRepository jobRepository,
            SkillRepository skillRepository,
            AIService aiService,
            ObjectMapper objectMapper) {
        this.analysisRepository = analysisRepository;
        this.assessmentRepository = assessmentRepository;
        this.profileRepository = profileRepository;
        this.jobRepository = jobRepository;
        this.skillRepository = skillRepository;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ReadinessAnalysis performReadinessAnalysis(User user, UUID jobId) {
        CareerProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    CareerProfile newProfile = CareerProfile.builder()
                            .user(user)
                            .firstName("Candidate")
                            .lastName("")
                            .summary("Computer Science graduate with coursework, academic projects, and software engineering foundations.")
                            .targetRoles("Software Engineer, Backend Developer, Full Stack Engineer")
                            .educations(new ArrayList<>())
                            .experiences(new ArrayList<>())
                            .projects(new ArrayList<>())
                            .certifications(new ArrayList<>())
                            .skills(new ArrayList<>())
                            .build();
                    return profileRepository.save(newProfile);
                });

        Job job = jobRepository.findByIdAndUserId(jobId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        String systemInstruction = """
                You are a career readiness copilot. Analyze the user's Master Career Profile against the Job Description.
                Categorize each important skill in the job requirements into one of these 4 buckets:
                1. DEMONSTRATED: Clearly proven in project, internship, coursework, or certifications.
                2. HIDDEN_BUT_LIKELY: Implied by the context but not explicitly stated (e.g. REST APIs implied by a Spring Boot database project).
                3. CONFIRMED_BUT_WEAK: User says they know it, but has no projects, experience, or certifications proving it.
                4. GENUINE_GAP: Skill is required but not mentioned at all in the profile.
                
                Compute a readiness recommendation:
                - APPLY_NOW: Student has enough relevant skills and evidence.
                - APPLY_AFTER_TAILORING: Student is qualified, but existing resume fails to clearly state relevant skills.
                - STRETCH_APPLICATION: Student has some foundations, but key skills are weak or unproven.
                - BUILD_BEFORE_APPLYING: Significant genuine skill gaps exist that must be fixed first.
                
                Return JSON schema exactly as follows:
                {
                  "eligibilitySummary": "text block",
                  "technicalFitSummary": "text block",
                  "evidenceStrengthSummary": "text block",
                  "presentationSummary": "text block",
                  "recommendation": "APPLY_NOW / APPLY_AFTER_TAILORING / STRETCH_APPLICATION / BUILD_BEFORE_APPLYING",
                  "skills": [
                    {
                      "name": "Skill Name",
                      "classification": "DEMONSTRATED / HIDDEN_BUT_LIKELY / CONFIRMED_BUT_WEAK / GENUINE_GAP",
                      "details": "Explanation of why this bucket was chosen."
                    }
                  ]
                }
                """;

        String prompt = buildPrompt(profile, job);
        String jsonResponse = aiService.generateContent(prompt, systemInstruction, "READINESS", user.getId());

        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            ReadinessAnalysis analysis = ReadinessAnalysis.builder()
                    .user(user)
                    .job(job)
                    .eligibilitySummary(rootNode.path("eligibilitySummary").asText())
                    .technicalFitSummary(rootNode.path("technicalFitSummary").asText())
                    .evidenceStrengthSummary(rootNode.path("evidenceStrengthSummary").asText())
                    .presentationSummary(rootNode.path("presentationSummary").asText())
                    .recommendation(rootNode.path("recommendation").asText("STRETCH_APPLICATION"))
                    .skillAssessments(new ArrayList<>())
                    .build();

            analysis = analysisRepository.save(analysis);

            JsonNode skillsNode = rootNode.path("skills");
            if (skillsNode.isArray()) {
                for (JsonNode sNode : skillsNode) {
                    String skillName = sNode.path("name").asText().trim();
                    String classification = sNode.path("classification").asText("GENUINE_GAP");
                    String details = sNode.path("details").asText();

                    if (skillName.isEmpty()) continue;

                    Skill skill = skillRepository.findByNameIgnoreCase(skillName)
                            .orElseGet(() -> {
                                Skill newSkill = Skill.builder()
                                        .name(skillName)
                                        .category("General")
                                        .build();
                                return skillRepository.save(newSkill);
                            });

                    SkillAssessment assessment = SkillAssessment.builder()
                            .readinessAnalysis(analysis)
                            .skill(skill)
                            .classification(classification)
                            .details(details)
                            .build();

                    assessmentRepository.save(assessment);
                    analysis.getSkillAssessments().add(assessment);
                }
            }

            return analysis;

        } catch (Exception ex) {
            throw new RuntimeException("Failed to process readiness analysis: " + ex.getMessage(), ex);
        }
    }

    @Transactional(readOnly = true)
    public ReadinessAnalysis getLatestAnalysis(UUID jobId, UUID userId) {
        return analysisRepository.findFirstByJobIdAndUserIdOrderByCreatedAtDesc(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("No readiness analysis exists for this job yet."));
    }

    private String buildPrompt(CareerProfile profile, Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- JOB PROFILE ---\n");
        sb.append("Title: ").append(job.getTitle()).append("\n");
        sb.append("Company: ").append(job.getCompany()).append("\n");
        sb.append("Job Requirements:\n");
        job.getRequirements().forEach(r -> sb.append("- [").append(r.getType()).append("] ").append(r.getDescription()).append("\n"));
        
        sb.append("\n--- MASTER CAREER PROFILE ---\n");
        sb.append("Name: ").append(profile.getFirstName()).append(" ").append(profile.getLastName()).append("\n");
        sb.append("Summary: ").append(profile.getSummary()).append("\n");
        
        sb.append("Education:\n");
        profile.getEducations().forEach(e -> sb.append("- ").append(e.getDegree()).append(" in ").append(e.getFieldOfStudy()).append(" from ").append(e.getSchool()).append(" (GPA: ").append(e.getGpa()).append(")\n"));
        
        sb.append("Projects:\n");
        profile.getProjects().forEach(p -> sb.append("- ").append(p.getTitle()).append(" (Role: ").append(p.getRole()).append("): ").append(p.getDescription()).append("\n"));
        
        sb.append("Experiences:\n");
        profile.getExperiences().forEach(exp -> sb.append("- ").append(exp.getRole()).append(" at ").append(exp.getCompany()).append(": ").append(exp.getDescription()).append("\n"));
        
        sb.append("Certifications:\n");
        profile.getCertifications().forEach(c -> sb.append("- ").append(c.getName()).append(" from ").append(c.getIssuingOrganization()).append("\n"));
        
        sb.append("Self-declared Skills:\n");
        profile.getSkills().forEach(s -> sb.append("- ").append(s.getSkill().getName()).append(" (Level: ").append(s.getSelfAssessedLevel()).append(")\n"));

        return sb.toString();
    }
}

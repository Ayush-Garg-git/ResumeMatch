package com.jobreadiness.copilot.evidence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobreadiness.copilot.ai.service.AIService;
import com.jobreadiness.copilot.careerprofile.entity.CareerProfile;
import com.jobreadiness.copilot.careerprofile.entity.ProfileSkill;
import com.jobreadiness.copilot.careerprofile.repository.CareerProfileRepository;
import com.jobreadiness.copilot.common.exception.ResourceNotFoundException;
import com.jobreadiness.copilot.evidence.dto.EvidenceDto;
import com.jobreadiness.copilot.evidence.entity.Evidence;
import com.jobreadiness.copilot.evidence.repository.EvidenceRepository;
import com.jobreadiness.copilot.readiness.entity.ReadinessAnalysis;
import com.jobreadiness.copilot.readiness.entity.SkillAssessment;
import com.jobreadiness.copilot.readiness.repository.ReadinessAnalysisRepository;
import com.jobreadiness.copilot.skill.entity.Skill;
import com.jobreadiness.copilot.skill.repository.SkillRepository;
import com.jobreadiness.copilot.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final ReadinessAnalysisRepository readinessAnalysisRepository;
    private final CareerProfileRepository profileRepository;
    private final SkillRepository skillRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    public EvidenceService(
            EvidenceRepository evidenceRepository,
            ReadinessAnalysisRepository readinessAnalysisRepository,
            CareerProfileRepository profileRepository,
            SkillRepository skillRepository,
            AIService aiService,
            ObjectMapper objectMapper) {
        this.evidenceRepository = evidenceRepository;
        this.readinessAnalysisRepository = readinessAnalysisRepository;
        this.profileRepository = profileRepository;
        this.skillRepository = skillRepository;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public String discoverEvidenceQuestions(UUID jobId, UUID userId) {
        ReadinessAnalysis latestAnalysis = readinessAnalysisRepository.findFirstByJobIdAndUserIdOrderByCreatedAtDesc(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("No readiness analysis exists for this job. Run analysis first."));

        CareerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        List<SkillAssessment> targets = latestAnalysis.getSkillAssessments().stream()
                .filter(sa -> "HIDDEN_BUT_LIKELY".equalsIgnoreCase(sa.getClassification())
                        || "CONFIRMED_BUT_WEAK".equalsIgnoreCase(sa.getClassification()))
                .collect(Collectors.toList());

        if (targets.isEmpty()) {
            return "{\"questions\": []}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Student profile context:\n");
        profile.getProjects().forEach(p -> sb.append("- Project: ").append(p.getTitle()).append(": ").append(p.getDescription()).append("\n"));
        profile.getExperiences().forEach(exp -> sb.append("- Experience: ").append(exp.getRole()).append(" at ").append(exp.getCompany()).append(": ").append(exp.getDescription()).append("\n"));

        sb.append("\nSkills to discover evidence for:\n");
        targets.forEach(t -> sb.append("- ").append(t.getSkill().getName()).append(" (Status: ").append(t.getClassification()).append(")\n"));

        String systemInstruction = """
                You are a technical career guide. Generate targeted questions for skills that are either 'HIDDEN_BUT_LIKELY' or 'CONFIRMED_BUT_WEAK' on the student's profile.
                Do not ask simple yes/no questions like "Do you know Spring Boot?".
                Instead, query details such as:
                - Where did they use it (which project/experience)?
                - What endpoints/libraries did they write?
                - What database/infrastructure did they connect to?
                - Did they deploy it or implement specific components (validation, authorization)?
                
                Return JSON schema format:
                {
                  "questions": [
                    {
                      "question": "The question string...",
                      "context": "The skill name, e.g. Spring Boot",
                      "suggestedType": "PROJECT / INTERNSHIP / COURSEWORK / CERTIFICATION / GITHUB / PORTFOLIO / OTHER"
                    }
                  ]
                }
                """;

        return aiService.generateContent(sb.toString(), systemInstruction, "EVIDENCE_DISCOVERY", userId);
    }

    @Transactional
    public Evidence saveEvidence(User user, EvidenceDto dto) {
        Skill skill = skillRepository.findByNameIgnoreCase(dto.getSkillName())
                .orElseGet(() -> {
                    Skill newSkill = Skill.builder()
                            .name(dto.getSkillName())
                            .category("General")
                            .build();
                    return skillRepository.save(newSkill);
                });

        Evidence evidence = Evidence.builder()
                .user(user)
                .skill(skill)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .evidenceType(dto.getEvidenceType() != null ? dto.getEvidenceType() : "STUDENT_RESPONSE")
                .sourceType(dto.getSourceType() != null ? dto.getSourceType() : "NONE")
                .sourceId(dto.getSourceId())
                .confidence(dto.getConfidence() != null ? dto.getConfidence() : "MEDIUM")
                .studentConfirmed(dto.getStudentConfirmed() != null ? dto.getStudentConfirmed() : true)
                .build();

        evidence = evidenceRepository.save(evidence);

        CareerProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        boolean skillExists = profile.getSkills().stream()
                .anyMatch(ps -> ps.getSkill().getId().equals(skill.getId()));

        if (!skillExists) {
            ProfileSkill profileSkill = ProfileSkill.builder()
                    .id(new ProfileSkill.ProfileSkillId(profile.getId(), skill.getId()))
                    .profile(profile)
                    .skill(skill)
                    .selfAssessedLevel(dto.getConfidence())
                    .build();
            profile.getSkills().add(profileSkill);
            profileRepository.save(profile);
        }

        return evidence;
    }

    @Transactional(readOnly = true)
    public List<Evidence> getEvidenceBank(UUID userId) {
        return evidenceRepository.findByUserId(userId);
    }
}

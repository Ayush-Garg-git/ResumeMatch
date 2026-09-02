package com.jobreadiness.copilot.roadmap.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobreadiness.copilot.ai.service.AIService;
import com.jobreadiness.copilot.common.exception.ResourceNotFoundException;
import com.jobreadiness.copilot.proof.dto.ProofSubmissionDto;
import com.jobreadiness.copilot.proof.entity.ProofSubmission;
import com.jobreadiness.copilot.proof.repository.ProofSubmissionRepository;
import com.jobreadiness.copilot.readiness.entity.ReadinessAnalysis;
import com.jobreadiness.copilot.readiness.entity.SkillAssessment;
import com.jobreadiness.copilot.readiness.repository.ReadinessAnalysisRepository;
import com.jobreadiness.copilot.roadmap.entity.LearningMission;
import com.jobreadiness.copilot.roadmap.repository.LearningMissionRepository;
import com.jobreadiness.copilot.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RoadmapService {

    private final LearningMissionRepository missionRepository;
    private final ProofSubmissionRepository proofRepository;
    private final ReadinessAnalysisRepository readinessAnalysisRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    public RoadmapService(
            LearningMissionRepository missionRepository,
            ProofSubmissionRepository proofRepository,
            ReadinessAnalysisRepository readinessAnalysisRepository,
            AIService aiService,
            ObjectMapper objectMapper) {
        this.missionRepository = missionRepository;
        this.proofRepository = proofRepository;
        this.readinessAnalysisRepository = readinessAnalysisRepository;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<LearningMission> generateRoadmapForJob(User user, UUID jobId) {
        ReadinessAnalysis analysis = readinessAnalysisRepository.findFirstByJobIdAndUserIdOrderByCreatedAtDesc(jobId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No readiness analysis exists for this job. Run analysis first."));

        List<SkillAssessment> gaps = analysis.getSkillAssessments().stream()
                .filter(sa -> "GENUINE_GAP".equalsIgnoreCase(sa.getClassification()))
                .toList();

        List<LearningMission> generatedMissions = new ArrayList<>();

        for (var assessment : gaps) {
            if (missionRepository.findByUserIdAndSkillId(user.getId(), assessment.getSkill().getId()).isPresent()) {
                continue;
            }

            String systemInstruction = """
                    You are an expert technical mentor. Generate a structured learning roadmap mission for a student who lacks a specific technical skill.
                    
                    Return JSON schema format:
                    {
                      "title": "Title of the mission, e.g. Containerize Spring Boot app with Docker",
                      "priority": "HIGH / MEDIUM / LOW",
                      "explanation": "Why this skill matters for backend/data roles",
                      "resources": "Documentation links and free video titles/tutorials",
                      "practiceQuestions": "Practice interview/concept questions",
                      "miniProjectDesc": "Description of a realistic coding/configuration task the student should complete to prove the skill",
                      "deliverablesDesc": "What the student must submit (e.g. Dockerfile, code snippets)"
                    }
                    """;

            String prompt = String.format("Generate a learning mission for the skill: '%s' which is required for the job role: '%s'.",
                    assessment.getSkill().getName(), analysis.getJob().getTitle());

            try {
                String jsonResponse = aiService.generateContent(prompt, systemInstruction, "ROADMAP", user.getId());
                JsonNode rootNode = objectMapper.readTree(jsonResponse);

                LearningMission mission = LearningMission.builder()
                        .user(user)
                        .skill(assessment.getSkill())
                        .title(rootNode.path("title").asText("Learn " + assessment.getSkill().getName()))
                        .priority(rootNode.path("priority").asText("MEDIUM"))
                        .explanation(rootNode.path("explanation").asText())
                        .docLinks(rootNode.path("resources").asText())
                        .resources(rootNode.path("resources").asText())
                        .practiceQuestions(rootNode.path("practiceQuestions").asText())
                        .miniProjectDesc(rootNode.path("miniProjectDesc").asText())
                        .deliverablesDesc(rootNode.path("deliverablesDesc").asText())
                        .status("EXPLORED")
                        .build();

                missionRepository.save(mission);
                generatedMissions.add(mission);

            } catch (Exception ex) {
                System.err.println("Failed to generate learning mission for: " + assessment.getSkill().getName() + ". Error: " + ex.getMessage());
            }
        }

        return generatedMissions;
    }

    @Transactional
    public ProofSubmission submitProof(User user, UUID missionId, ProofSubmissionDto dto) {
        LearningMission mission = missionRepository.findByIdAndUserId(missionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Learning mission not found"));

        ProofSubmission submission = ProofSubmission.builder()
                .learningMission(mission)
                .proofType(dto.getProofType())
                .proofUrl(dto.getProofUrl())
                .proofContent(dto.getProofContent())
                .status("APPROVED")
                .feedback("Excellent work! Skill validated.")
                .build();

        submission = proofRepository.save(submission);

        mission.setStatus("VERIFIED");
        missionRepository.save(mission);

        return submission;
    }

    @Transactional(readOnly = true)
    public List<LearningMission> getRoadmap(UUID userId) {
        return missionRepository.findByUserId(userId);
    }
}

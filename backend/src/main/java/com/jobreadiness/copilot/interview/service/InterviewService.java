package com.jobreadiness.copilot.interview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobreadiness.copilot.ai.service.AIService;
import com.jobreadiness.copilot.claim.entity.Claim;
import com.jobreadiness.copilot.claim.repository.ClaimRepository;
import com.jobreadiness.copilot.common.exception.BadRequestException;
import com.jobreadiness.copilot.common.exception.ResourceNotFoundException;
import com.jobreadiness.copilot.interview.entity.InterviewQuestion;
import com.jobreadiness.copilot.interview.repository.InterviewQuestionRepository;
import com.jobreadiness.copilot.job.entity.Job;
import com.jobreadiness.copilot.job.repository.JobRepository;
import com.jobreadiness.copilot.tailoring.entity.TailoringSession;
import com.jobreadiness.copilot.tailoring.repository.TailoringSessionRepository;
import com.jobreadiness.copilot.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class InterviewService {

    private final InterviewQuestionRepository questionRepository;
    private final TailoringSessionRepository sessionRepository;
    private final ClaimRepository claimRepository;
    private final JobRepository jobRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    public InterviewService(
            InterviewQuestionRepository questionRepository,
            TailoringSessionRepository sessionRepository,
            ClaimRepository claimRepository,
            JobRepository jobRepository,
            AIService aiService,
            ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.sessionRepository = sessionRepository;
        this.claimRepository = claimRepository;
        this.jobRepository = jobRepository;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<InterviewQuestion> generateQuestionsForJob(User user, UUID jobId) {
        Job job = jobRepository.findByIdAndUserId(jobId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        List<TailoringSession> sessions = sessionRepository.findByUserId(user.getId()).stream()
                .filter(s -> s.getJob().getId().equals(jobId))
                .toList();

        List<Claim> approvedClaims = new ArrayList<>();
        for (var session : sessions) {
            List<Claim> claims = claimRepository.findByTailoringSessionIdAndUserId(session.getId(), user.getId());
            approvedClaims.addAll(claims);
        }

        List<InterviewQuestion> oldQuestions = questionRepository.findByJobIdAndUserId(jobId, user.getId());
        questionRepository.deleteAll(oldQuestions);

        StringBuilder sb = new StringBuilder();
        sb.append("Job Title: ").append(job.getTitle()).append("\n");
        sb.append("Company: ").append(job.getCompany()).append("\n\n");
        if (!approvedClaims.isEmpty()) {
            sb.append("Resume Claims to Defend:\n");
            approvedClaims.forEach(c -> sb.append("- Claim: '").append(c.getProposedText() != null ? c.getProposedText() : c.getOriginalText()).append("'\n"));
        } else {
            sb.append("Job Requirements to Defend:\n");
            job.getRequirements().forEach(r -> sb.append("- [").append(r.getType()).append("] ").append(r.getDescription()).append("\n"));
        }

        String systemInstruction = """
                You are a senior technical interviewer. Generate mock interview questions to verify and test the student on their approved claims.
                For each claim, create 1-2 realistic, challenging interview questions that test whether the student actually built/understands what is written.
                Provide brief guidelines on how they should answer (the key technical points they must cover to defend the claim).
                
                Return JSON schema format:
                {
                  "questions": [
                    {
                      "claimId": "UUID of the claim",
                      "question": "The question string...",
                      "guideline": "Answer guideline summary..."
                    }
                  ]
                }
                """;

        List<InterviewQuestion> createdQuestions = new ArrayList<>();
        try {
            String jsonResponse = aiService.generateContent(sb.toString(), systemInstruction, "INTERVIEW_PREP", user.getId());
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            JsonNode questionsNode = rootNode.path("questions");
            if (questionsNode.isArray()) {
                for (JsonNode qNode : questionsNode) {
                    UUID claimId = null;
                    try {
                        String cid = qNode.path("claimId").asText(null);
                        if (cid != null && cid.trim().length() == 36) {
                            claimId = UUID.fromString(cid.trim());
                        }
                    } catch (Exception ignored) {}

                    String questionText = qNode.path("question").asText();
                    String guideline = qNode.path("guideline").asText();

                    Claim claim = null;
                    if (claimId != null) {
                        UUID finalClaimId = claimId;
                        claim = approvedClaims.stream()
                                .filter(c -> c.getId().equals(finalClaimId))
                                .findFirst()
                                .orElse(null);
                    }

                    InterviewQuestion question = InterviewQuestion.builder()
                            .user(user)
                            .job(job)
                            .claim(claim)
                            .question(questionText)
                            .suggestedAnswerGuideline(guideline)
                            .build();

                    questionRepository.save(question);
                    createdQuestions.add(question);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate mock interview questions: " + ex.getMessage(), ex);
        }

        return createdQuestions;
    }

    @Transactional(readOnly = true)
    public List<InterviewQuestion> getQuestionsForJob(UUID jobId, UUID userId) {
        return questionRepository.findByJobIdAndUserId(jobId, userId);
    }
}

package com.jobreadiness.copilot.tailoring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobreadiness.copilot.ai.service.AIService;
import com.jobreadiness.copilot.careerprofile.entity.CareerProfile;
import com.jobreadiness.copilot.careerprofile.repository.CareerProfileRepository;
import com.jobreadiness.copilot.claim.entity.Claim;
import com.jobreadiness.copilot.claim.repository.ClaimRepository;
import com.jobreadiness.copilot.common.exception.ResourceNotFoundException;
import com.jobreadiness.copilot.evidence.entity.Evidence;
import com.jobreadiness.copilot.evidence.repository.EvidenceRepository;
import com.jobreadiness.copilot.job.entity.Job;
import com.jobreadiness.copilot.job.repository.JobRepository;
import com.jobreadiness.copilot.resume.entity.Resume;
import com.jobreadiness.copilot.resume.entity.ResumeVersion;
import com.jobreadiness.copilot.resume.repository.ResumeRepository;
import com.jobreadiness.copilot.resume.repository.ResumeVersionRepository;
import com.jobreadiness.copilot.tailoring.dto.StartSessionRequest;
import com.jobreadiness.copilot.tailoring.entity.TailoringSession;
import com.jobreadiness.copilot.tailoring.repository.TailoringSessionRepository;
import com.jobreadiness.copilot.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TailoringService {

    private final TailoringSessionRepository sessionRepository;
    private final ClaimRepository claimRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository versionRepository;
    private final JobRepository jobRepository;
    private final EvidenceRepository evidenceRepository;
    private final CareerProfileRepository profileRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    public TailoringService(
            TailoringSessionRepository sessionRepository,
            ClaimRepository claimRepository,
            ResumeRepository resumeRepository,
            ResumeVersionRepository versionRepository,
            JobRepository jobRepository,
            EvidenceRepository evidenceRepository,
            CareerProfileRepository profileRepository,
            AIService aiService,
            ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.claimRepository = claimRepository;
        this.resumeRepository = resumeRepository;
        this.versionRepository = versionRepository;
        this.jobRepository = jobRepository;
        this.evidenceRepository = evidenceRepository;
        this.profileRepository = profileRepository;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TailoringSession startSession(User user, StartSessionRequest request) {
        Job job = jobRepository.findByIdAndUserId(request.getJobId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        Resume resume = resumeRepository.findByIdAndUserId(request.getResumeId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        TailoringSession session = TailoringSession.builder()
                .user(user)
                .job(job)
                .sourceResume(resume)
                .status("PENDING")
                .claims(new ArrayList<>())
                .build();
        session = sessionRepository.save(session);

        generateClaimProposals(session, user.getId());

        return session;
    }

    private void generateClaimProposals(TailoringSession session, UUID userId) {
        CareerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        List<Evidence> evidences = evidenceRepository.findByUserId(userId);

        StringBuilder sb = new StringBuilder();
        sb.append("Original Projects:\n");
        profile.getProjects().forEach(p -> sb.append("- [PROJECT] ").append(p.getTitle()).append(": ").append(p.getDescription()).append("\n"));
        
        sb.append("\nOriginal Experiences:\n");
        profile.getExperiences().forEach(exp -> sb.append("- [EXPERIENCE] ").append(exp.getRole()).append(" at ").append(exp.getCompany()).append(": ").append(exp.getDescription()).append("\n"));

        sb.append("\nStudent Verified Evidence Bank:\n");
        evidences.stream().filter(Evidence::getStudentConfirmed).forEach(ev -> 
            sb.append("- Skill: ").append(ev.getSkill().getName())
              .append(", Source: ").append(ev.getSourceType()).append(" (").append(ev.getTitle()).append(")")
              .append(", Evidence: ").append(ev.getDescription()).append("\n")
        );

        sb.append("\nTarget Job Requirements:\n");
        session.getJob().getRequirements().forEach(r -> sb.append("- ").append(r.getDescription()).append("\n"));

        String systemInstruction = """
                You are a resume tailoring system. Analyze the original project bullets/experiences and match them with the verified student evidence.
                Suggest enhanced bullet points that align the student's REAL accomplishments to the target job description.
                
                CRITICAL SAFETY RULES:
                - Do NOT invent metrics (e.g. do not add 'increased performance by 40%' if it was not in the original text or evidence).
                - Do NOT invent technologies (only use tools explicitly declared in the original profile or verified evidence).
                - Do NOT invent projects or responsibilities.
                - Keep changes truthful and realistic.
                
                Return JSON schema format:
                {
                  "claims": [
                    {
                      "originalText": "The exact matching bullet or sentence from the original project/experience",
                      "proposedText": "The enhanced, tailored version of that bullet integrating verified evidence",
                      "confidence": "STRONG / MEDIUM / WEAK"
                    }
                  ]
                }
                """;

        try {
            String jsonResponse = aiService.generateContent(sb.toString(), systemInstruction, "CLAIM_GEN", userId);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            JsonNode claimsNode = rootNode.path("claims");
            if (claimsNode.isArray()) {
                for (JsonNode cNode : claimsNode) {
                    Claim claim = Claim.builder()
                            .user(session.getUser())
                            .tailoringSession(session)
                            .originalText(cNode.path("originalText").asText())
                            .proposedText(cNode.path("proposedText").asText())
                            .status("PENDING")
                            .confidence(cNode.path("confidence").asText("MEDIUM"))
                            .build();

                    claimRepository.save(claim);
                    session.getClaims().add(claim);
                }
            }
        } catch (Exception ex) {
            System.err.println("Failed to suggest tailoring claims: " + ex.getMessage());
        }
    }

    @Transactional
    public Claim updateClaim(User user, UUID claimId, String status, String editedProposedText) {
        Claim claim = claimRepository.findByIdAndUserId(claimId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found"));

        if (status != null) {
            claim.setStatus(status.toUpperCase());
        }
        if (editedProposedText != null && !editedProposedText.trim().isEmpty()) {
            claim.setProposedText(editedProposedText);
            claim.setStatus("EDITED");
        }

        return claimRepository.save(claim);
    }

    @Transactional
    public ResumeVersion compileTailoredResume(User user, UUID sessionId) {
        TailoringSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tailoring session not found"));

        List<Claim> approvedClaims = claimRepository.findByTailoringSessionIdAndUserId(sessionId, user.getId()).stream()
                .filter(c -> "APPROVED".equals(c.getStatus()) || "EDITED".equals(c.getStatus()))
                .collect(Collectors.toList());

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Original Resume Text:\n").append(session.getSourceResume().getRawText()).append("\n\n");
        promptBuilder.append("Approved claim replacements (find matching lines in original and replace them):\n");
        approvedClaims.forEach(c -> promptBuilder.append("- Replace: '").append(c.getOriginalText()).append("'\n  With: '").append(c.getProposedText()).append("'\n"));

        String systemInstruction = """
                You are a resume editor. Your job is to take the original raw resume text and replace the original statements with the approved, tailored claims.
                Keep all other details (dates, names, schools, other bullets) exactly the same.
                Do not invent anything else.
                Return the fully compiled tailored resume in plain text.
                """;

        String compiledText = aiService.generateContent(promptBuilder.toString(), systemInstruction, "RESUME_TAILOR", user.getId());

        List<ResumeVersion> versions = versionRepository.findByResumeIdAndUserId(session.getSourceResume().getId(), user.getId());
        int nextVer = versions.size() + 1;
        String relativePath = "resumes/" + user.getId() + "/tailored_v" + nextVer + ".txt";

        ResumeVersion tailoredVersion = ResumeVersion.builder()
                .resume(session.getSourceResume())
                .user(user)
                .versionNumber(nextVer)
                .filePath(relativePath)
                .contentJson(compiledText)
                .isTailored(true)
                .build();

        tailoredVersion = versionRepository.save(tailoredVersion);

        session.setTailoredResumeVersion(tailoredVersion);
        session.setStatus("COMPLETED");
        sessionRepository.save(session);

        return tailoredVersion;
    }
}

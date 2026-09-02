package com.jobreadiness.copilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobreadiness.copilot.ai.service.AIService;
import com.jobreadiness.copilot.auth.dto.SignupRequest;
import com.jobreadiness.copilot.auth.service.AuthService;
import com.jobreadiness.copilot.careerprofile.entity.CareerProfile;
import com.jobreadiness.copilot.careerprofile.repository.CareerProfileRepository;
import com.jobreadiness.copilot.claim.entity.Claim;
import com.jobreadiness.copilot.claim.repository.ClaimRepository;
import com.jobreadiness.copilot.common.exception.BadRequestException;
import com.jobreadiness.copilot.common.security.JwtTokenProvider;
import com.jobreadiness.copilot.job.entity.Job;
import com.jobreadiness.copilot.job.repository.JobRepository;
import com.jobreadiness.copilot.readiness.entity.ReadinessAnalysis;
import com.jobreadiness.copilot.readiness.repository.ReadinessAnalysisRepository;
import com.jobreadiness.copilot.readiness.repository.SkillAssessmentRepository;
import com.jobreadiness.copilot.readiness.service.ReadinessService;
import com.jobreadiness.copilot.skill.repository.SkillRepository;
import com.jobreadiness.copilot.tailoring.service.TailoringService;
import com.jobreadiness.copilot.user.entity.User;
import com.jobreadiness.copilot.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CopilotBusinessTests {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private ClaimRepository claimRepository;
    @Mock private CareerProfileRepository profileRepository;
    @Mock private JobRepository jobRepository;
    @Mock private ReadinessAnalysisRepository analysisRepository;
    @Mock private SkillAssessmentRepository assessmentRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private AIService aiService;

    @InjectMocks private AuthService authService;
    @InjectMocks private TailoringService tailoringService;
    @InjectMocks private ReadinessService readinessService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        readinessService = new ReadinessService(
                analysisRepository,
                assessmentRepository,
                profileRepository,
                jobRepository,
                skillRepository,
                aiService,
                objectMapper
        );
    }

    @Test
    public void testSignupDuplicateEmailThrowsBadRequest() {
        SignupRequest request = new SignupRequest();
        request.setEmail("test@email.com");
        request.setPassword("password");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.signup(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testClaimUpdateWorkflow() {
        User user = User.builder().id(UUID.randomUUID()).email("test@email.com").build();
        Claim claim = Claim.builder()
                .id(UUID.randomUUID())
                .user(user)
                .originalText("Old bullet")
                .proposedText("New bullet")
                .status("PENDING")
                .build();

        when(claimRepository.findByIdAndUserId(claim.getId(), user.getId())).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Claim approved = tailoringService.updateClaim(user, claim.getId(), "APPROVED", null);
        assertEquals("APPROVED", approved.getStatus());

        Claim edited = tailoringService.updateClaim(user, claim.getId(), null, "Custom bullet text");
        assertEquals("EDITED", edited.getStatus());
        assertEquals("Custom bullet text", edited.getProposedText());
    }

    @Test
    public void testReadinessBucketClassification() {
        User user = User.builder().id(UUID.randomUUID()).email("test@email.com").build();
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder().id(jobId).title("Backend Dev").build();
        CareerProfile profile = CareerProfile.builder().id(UUID.randomUUID()).user(user).build();

        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(jobRepository.findByIdAndUserId(jobId, user.getId())).thenReturn(Optional.of(job));
        when(analysisRepository.save(any(ReadinessAnalysis.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String mockAiResponse = """
        {
          "eligibilitySummary": "Eligible",
          "technicalFitSummary": "Good fit",
          "evidenceStrengthSummary": "Moderate",
          "presentationSummary": "Clean summary",
          "recommendation": "APPLY_AFTER_TAILORING",
          "skills": [
            {
              "name": "Java",
              "classification": "DEMONSTRATED",
              "details": "Used in e-commerce backend"
            },
            {
              "name": "Docker",
              "classification": "GENUINE_GAP",
              "details": "No container experience"
            }
          ]
        }
        """;
        when(aiService.generateContent(any(), any(), eq("READINESS"), eq(user.getId()))).thenReturn(mockAiResponse);

        ReadinessAnalysis result = readinessService.performReadinessAnalysis(user, jobId);

        assertNotNull(result);
        assertEquals("APPLY_AFTER_TAILORING", result.getRecommendation());
        assertEquals("Eligible", result.getEligibilitySummary());
        verify(analysisRepository, times(1)).save(any(ReadinessAnalysis.class));
    }
}

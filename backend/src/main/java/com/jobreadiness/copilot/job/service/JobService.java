package com.jobreadiness.copilot.job.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobreadiness.copilot.ai.service.AIService;
import com.jobreadiness.copilot.common.exception.ResourceNotFoundException;
import com.jobreadiness.copilot.job.dto.CreateJobRequest;
import com.jobreadiness.copilot.job.dto.JobDto;
import com.jobreadiness.copilot.job.entity.Job;
import com.jobreadiness.copilot.job.entity.JobRequirement;
import com.jobreadiness.copilot.job.mapper.JobMapper;
import com.jobreadiness.copilot.job.repository.JobRepository;
import com.jobreadiness.copilot.job.repository.JobRequirementRepository;
import com.jobreadiness.copilot.skill.entity.Skill;
import com.jobreadiness.copilot.skill.repository.SkillRepository;
import com.jobreadiness.copilot.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobRequirementRepository requirementRepository;
    private final SkillRepository skillRepository;
    private final AIService aiService;
    private final JobMapper mapper;
    private final ObjectMapper objectMapper;
    private final com.jobreadiness.copilot.job.queue.JdAnalysisProducer jdAnalysisProducer;

    public JobService(
            JobRepository jobRepository,
            JobRequirementRepository requirementRepository,
            SkillRepository skillRepository,
            AIService aiService,
            JobMapper mapper,
            ObjectMapper objectMapper,
            com.jobreadiness.copilot.job.queue.JdAnalysisProducer jdAnalysisProducer) {
        this.jobRepository = jobRepository;
        this.requirementRepository = requirementRepository;
        this.skillRepository = skillRepository;
        this.aiService = aiService;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.jdAnalysisProducer = jdAnalysisProducer;
    }

    @Transactional
    public JobDto createJob(User user, CreateJobRequest request) {
        boolean hasJdText = request.getRawJdText() != null && !request.getRawJdText().trim().isEmpty();
        String initialStatus = hasJdText ? "PROCESSING" : "COMPLETED";

        Job job = Job.builder()
                .user(user)
                .title(request.getTitle())
                .company(request.getCompany())
                .description(request.getDescription())
                .rawJdText(request.getRawJdText())
                .status(initialStatus)
                .requirements(new ArrayList<>())
                .build();
        job = jobRepository.saveAndFlush(job);

        if (hasJdText) {
            com.jobreadiness.copilot.job.queue.JdAnalysisMessage message = com.jobreadiness.copilot.job.queue.JdAnalysisMessage.builder()
                    .jobId(job.getId())
                    .userId(user.getId())
                    .rawJdText(request.getRawJdText())
                    .build();

            jdAnalysisProducer.sendJdAnalysisTask(message, msg -> processJdAnalysis(msg.getJobId(), msg.getRawJdText(), msg.getUserId()));
        }

        return mapper.toDto(job);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JobService.class);

    public void processJdAnalysis(UUID jobId, String jdText, UUID userId) {
        log.info("Starting processJdAnalysis for Job ID: {}", jobId);
        Job job = null;
        for (int i = 0; i < 5; i++) {
            job = jobRepository.findById(jobId).orElse(null);
            if (job != null) break;
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {}
        }

        if (job == null) {
            log.error("Job not found after retries for ID: {}", jobId);
            return;
        }

        try {
            log.info("Calling parseJobDescriptionWithAI for Job ID: {}", jobId);
            parseJobDescriptionWithAI(job, jdText, userId);
            job.setStatus("COMPLETED");
            log.info("Completed parseJobDescriptionWithAI, marked status COMPLETED for Job ID: {}", jobId);
        } catch (Exception e) {
            log.error("Error processing JD analysis for job {}: {}", jobId, e.getMessage(), e);
            job.setStatus("FAILED");
        }
        jobRepository.saveAndFlush(job);
    }

    private void parseJobDescriptionWithAI(Job job, String jdText, UUID userId) {
        String systemInstruction = """
                You are an expert technical recruiter parsing a Job Description. 
                Identify key requirements and return a structured JSON response. 
                Keep skill names canonical, clean, and concise (e.g. use "Java", "Spring Boot", "MySQL", "AWS", "Docker" - do not include descriptions in the skill name).
                
                JSON Format schema:
                {
                  "mustHaveSkills": ["SkillA", "SkillB"],
                  "preferredSkills": ["SkillC"],
                  "softSkills": ["SkillD"],
                  "responsibilities": ["Responsibility statement 1", "Responsibility statement 2"]
                }
                """;

        String prompt = "Parse the following Job Description text:\n\n" + jdText;

        try {
            log.info("Calling aiService.generateContent for JD_ANALYZE...");
            String jsonResponse = aiService.generateContent(prompt, systemInstruction, "JD_ANALYZE", userId);
            log.info("aiService response received: {}", jsonResponse);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            parseAndSaveSkills(job, rootNode.path("mustHaveSkills"), "MUST_HAVE");
            parseAndSaveSkills(job, rootNode.path("preferredSkills"), "PREFERRED");
            parseAndSaveSkills(job, rootNode.path("softSkills"), "SOFT_SKILL");

            JsonNode responsibilities = rootNode.path("responsibilities");
            if (responsibilities.isArray()) {
                for (JsonNode respNode : responsibilities) {
                    JobRequirement req = JobRequirement.builder()
                            .job(job)
                            .type("RESPONSIBILITY")
                            .description(respNode.asText())
                            .build();
                    requirementRepository.saveAndFlush(req);
                }
            }

        } catch (Exception ex) {
            log.error("Failed to parse JD with AI: {}", ex.getMessage(), ex);
        }
    }

    private void parseAndSaveSkills(Job job, JsonNode skillArrayNode, String type) {
        if (!skillArrayNode.isArray()) return;

        for (JsonNode skillNode : skillArrayNode) {
            String skillName = skillNode.asText().trim();
            if (skillName.isEmpty()) continue;

            Skill skill = skillRepository.findByNameIgnoreCase(skillName)
                    .orElseGet(() -> {
                        Skill newSkill = Skill.builder()
                                .name(skillName)
                                .category("General")
                                .build();
                        return skillRepository.saveAndFlush(newSkill);
                    });

            JobRequirement req = JobRequirement.builder()
                    .job(job)
                    .type(type)
                    .description("Required skill: " + skill.getName())
                    .skill(skill)
                    .build();
            requirementRepository.saveAndFlush(req);
        }
    }

    @Transactional(readOnly = true)
    public List<JobDto> getJobs(UUID userId) {
        return jobRepository.findByUserId(userId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobDto getJob(UUID id, UUID userId) {
        Job job = jobRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        return mapper.toDto(job);
    }
}

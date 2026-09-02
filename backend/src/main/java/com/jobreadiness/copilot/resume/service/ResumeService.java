package com.jobreadiness.copilot.resume.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobreadiness.copilot.ai.service.AIService;
import com.jobreadiness.copilot.careerprofile.entity.*;
import com.jobreadiness.copilot.careerprofile.repository.CareerProfileRepository;
import com.jobreadiness.copilot.common.exception.BadRequestException;
import com.jobreadiness.copilot.common.exception.ResourceNotFoundException;
import com.jobreadiness.copilot.resume.entity.Resume;
import com.jobreadiness.copilot.resume.entity.ResumeVersion;
import com.jobreadiness.copilot.resume.repository.ResumeRepository;
import com.jobreadiness.copilot.resume.repository.ResumeVersionRepository;
import com.jobreadiness.copilot.resume.util.DocumentTextExtractor;
import com.jobreadiness.copilot.skill.entity.Skill;
import com.jobreadiness.copilot.skill.repository.SkillRepository;
import com.jobreadiness.copilot.storage.FileStorageService;
import com.jobreadiness.copilot.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository versionRepository;
    private final FileStorageService fileStorageService;
    private final DocumentTextExtractor textExtractor;
    private final AIService aiService;
    private final CareerProfileRepository profileRepository;
    private final SkillRepository skillRepository;
    private final ObjectMapper objectMapper;

    public ResumeService(
            ResumeRepository resumeRepository,
            ResumeVersionRepository versionRepository,
            FileStorageService fileStorageService,
            DocumentTextExtractor textExtractor,
            AIService aiService,
            CareerProfileRepository profileRepository,
            SkillRepository skillRepository,
            ObjectMapper objectMapper) {
        this.resumeRepository = resumeRepository;
        this.versionRepository = versionRepository;
        this.fileStorageService = fileStorageService;
        this.textExtractor = textExtractor;
        this.aiService = aiService;
        this.profileRepository = profileRepository;
        this.skillRepository = skillRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Resume uploadResume(User user, MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        // 1. Extract raw text from the file
        String extractedText = textExtractor.extractText(file);

        // 2. Save physical file to storage
        String relativePath = fileStorageService.storeFile(file, "resumes/" + user.getId());

        // 3. Save Resume record
        Resume resume = Resume.builder()
                .user(user)
                .filename(filename)
                .filePath(relativePath)
                .fileType(file.getContentType())
                .rawText(extractedText)
                .build();
        resume = resumeRepository.save(resume);

        // 4. Create ResumeVersion 1
        ResumeVersion version = ResumeVersion.builder()
                .resume(resume)
                .user(user)
                .versionNumber(1)
                .filePath(relativePath)
                .isTailored(false)
                .build();
        versionRepository.save(version);

        // 5. Parse ALL skills, technical proficiencies, and career details with Gemini AI
        parseAndPopulateProfileWithAi(user, extractedText);

        return resume;
    }

    private void parseAndPopulateProfileWithAi(User user, String resumeText) {
        if (resumeText == null || resumeText.isBlank()) return;

        String systemInstruction = "You are an elite, comprehensive AI Resume Parser for technical and engineering resumes. " +
                "Extract EVERY SINGLE skill, programming language, database, framework, library, tool, operating system, " +
                "hardware competency, networking protocol, and technology explicitly mentioned in the resume. " +
                "Do NOT omit any skill. For example, include hardware skills (PC Assembly, Troubleshooting, BIOS/UEFI), " +
                "networking (TCP/IP, LAN, DNS, DHCP, Routers/Switches), OS (Linux, Windows), databases (SQL, MySQL, PostgreSQL), " +
                "programming languages (Java, C++, Python, JavaScript, TypeScript), backend/web (Spring Boot, Spring MVC, REST APIs, Hibernate/JPA, JDBC, Node.js, HTML, CSS), " +
                "and tools (Git, GitHub, VS Code, Postman, Vercel, Google Cloud Vision, Gemini API, IoT, Sensors). " +
                "Return ONLY a valid JSON object matching this schema without markdown code fences:\n" +
                "{\n" +
                "  \"summary\": \"Extracted professional summary\",\n" +
                "  \"targetRoles\": \"Comma-separated target roles suggested by background\",\n" +
                "  \"skills\": [{\"name\": \"Skill Name\", \"category\": \"TECHNICAL\"}],\n" +
                "  \"educations\": [{\"school\": \"...\", \"degree\": \"...\", \"fieldOfStudy\": \"...\", \"gpa\": 0.0}],\n" +
                "  \"projects\": [{\"title\": \"...\", \"description\": \"...\", \"role\": \"...\"}],\n" +
                "  \"experiences\": [{\"company\": \"...\", \"role\": \"...\", \"description\": \"...\"}]\n" +
                "}";

        try {
            String jsonResponse = aiService.generateContent(resumeText, systemInstruction, "RESUME_PARSE", user.getId());
            if (jsonResponse == null || jsonResponse.isBlank()) return;

            // Clean markdown fences
            String cleanJson = jsonResponse.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            JsonNode rootNode = objectMapper.readTree(cleanJson);

            CareerProfile profile = profileRepository.findByUserId(user.getId())
                    .orElseGet(() -> CareerProfile.builder()
                            .user(user)
                            .educations(new ArrayList<>())
                            .projects(new ArrayList<>())
                            .experiences(new ArrayList<>())
                            .certifications(new ArrayList<>())
                            .skills(new ArrayList<>())
                            .build());

            if (profile.getSummary() == null || profile.getSummary().isBlank()) {
                String sum = rootNode.path("summary").asText(null);
                if (sum != null && !sum.isBlank()) profile.setSummary(sum);
            }
            if (profile.getTargetRoles() == null || profile.getTargetRoles().isBlank()) {
                String roles = rootNode.path("targetRoles").asText(null);
                if (roles != null && !roles.isBlank()) profile.setTargetRoles(roles);
            }

            // Sync all extracted skills
            JsonNode skillsNode = rootNode.path("skills");
            if (skillsNode.isArray()) {
                if (profile.getSkills() == null) {
                    profile.setSkills(new ArrayList<>());
                }

                Set<String> existingSkillNames = profile.getSkills().stream()
                        .map(ps -> ps.getSkill().getName().toLowerCase().trim())
                        .collect(Collectors.toSet());

                for (JsonNode skillElement : skillsNode) {
                    String skillName = skillElement.path("name").asText("").trim();
                    String category = skillElement.path("category").asText("TECHNICAL").trim();

                    if (!skillName.isBlank() && !existingSkillNames.contains(skillName.toLowerCase())) {
                        Skill skill = skillRepository.findByNameIgnoreCase(skillName)
                                .orElseGet(() -> skillRepository.save(Skill.builder()
                                        .name(skillName)
                                        .category(category)
                                        .build()));

                        ProfileSkill profileSkill = ProfileSkill.builder()
                                .id(new ProfileSkill.ProfileSkillId(profile.getId(), skill.getId()))
                                .profile(profile)
                                .skill(skill)
                                .selfAssessedLevel("PROFICIENT")
                                .build();

                        profile.getSkills().add(profileSkill);
                        existingSkillNames.add(skillName.toLowerCase());
                    }
                }
            }

            profileRepository.save(profile);
            log.info("Successfully extracted and synced {} skills from resume for user {}", 
                    profile.getSkills().size(), user.getId());
        } catch (Exception e) {
            log.warn("Non-fatal error parsing resume with AI: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Resume> getResumes(UUID userId) {
        return resumeRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Resume getResume(UUID id, UUID userId) {
        return resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
    }
}

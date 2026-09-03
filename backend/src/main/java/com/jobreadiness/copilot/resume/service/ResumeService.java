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
import java.util.*;
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

        // 5. Parse ALL skills, projects, experiences, and career details with Gemini AI and deterministic parser
        parseAndPopulateProfileWithAi(user, extractedText);

        return resume;
    }

    private void parseAndPopulateProfileWithAi(User user, String resumeText) {
        if (resumeText == null || resumeText.isBlank()) return;

        CareerProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> profileRepository.save(CareerProfile.builder()
                        .user(user)
                        .educations(new ArrayList<>())
                        .projects(new ArrayList<>())
                        .experiences(new ArrayList<>())
                        .certifications(new ArrayList<>())
                        .skills(new ArrayList<>())
                        .build()));

        if (profile.getSkills() == null) profile.setSkills(new ArrayList<>());
        if (profile.getProjects() == null) profile.setProjects(new ArrayList<>());
        if (profile.getExperiences() == null) profile.setExperiences(new ArrayList<>());
        if (profile.getEducations() == null) profile.setEducations(new ArrayList<>());

        Set<String> existingSkillNames = profile.getSkills().stream()
                .map(ps -> ps.getSkill().getName().toLowerCase().trim())
                .collect(Collectors.toSet());

        Set<String> existingProjectTitles = profile.getProjects().stream()
                .map(p -> p.getTitle().toLowerCase().trim())
                .collect(Collectors.toSet());

        // 1. Deterministic Extraction for Technical Skills & Projects
        extractDeterministicSkills(profile, resumeText, existingSkillNames);
        extractDeterministicProjects(profile, resumeText, existingProjectTitles);

        // 2. Comprehensive AI Extraction with Gemini
        String systemInstruction = "You are an elite, comprehensive AI Resume Parser for technical and engineering resumes. " +
                "Extract EVERY SINGLE skill, project, work experience, and educational credential mentioned in the resume. " +
                "Do NOT omit any project or technical skill. " +
                "Return ONLY a valid JSON object matching this schema without markdown code fences:\n" +
                "{\n" +
                "  \"summary\": \"Extracted professional summary\",\n" +
                "  \"targetRoles\": \"Comma-separated target roles suggested by background\",\n" +
                "  \"skills\": [{\"name\": \"Skill Name\", \"category\": \"TECHNICAL\"}],\n" +
                "  \"projects\": [{\"title\": \"Project Title\", \"role\": \"Role/Technologies\", \"description\": \"Detailed description of deliverables, implementation, architecture, and results.\"}],\n" +
                "  \"experiences\": [{\"company\": \"Company Name\", \"role\": \"Job Title\", \"description\": \"Responsibilities and technical contributions.\", \"startDate\": \"...\", \"endDate\": \"...\"}],\n" +
                "  \"educations\": [{\"school\": \"University / College\", \"degree\": \"Degree\", \"fieldOfStudy\": \"Major\", \"gpa\": 0.0}]\n" +
                "}";

        try {
            String jsonResponse = aiService.generateContent(resumeText, systemInstruction, "RESUME_PARSE", user.getId());
            if (jsonResponse != null && !jsonResponse.isBlank()) {
                String cleanJson = jsonResponse.trim();
                if (cleanJson.startsWith("```json")) cleanJson = cleanJson.substring(7);
                else if (cleanJson.startsWith("```")) cleanJson = cleanJson.substring(3);
                if (cleanJson.endsWith("```")) cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
                cleanJson = cleanJson.trim();

                JsonNode rootNode = objectMapper.readTree(cleanJson);

                if (profile.getSummary() == null || profile.getSummary().isBlank()) {
                    String sum = rootNode.path("summary").asText(null);
                    if (sum != null && !sum.isBlank()) profile.setSummary(sum);
                }
                if (profile.getTargetRoles() == null || profile.getTargetRoles().isBlank()) {
                    String roles = rootNode.path("targetRoles").asText(null);
                    if (roles != null && !roles.isBlank()) profile.setTargetRoles(roles);
                }

                // Parse Skills
                JsonNode skillsNode = rootNode.path("skills");
                if (skillsNode.isArray()) {
                    for (JsonNode skillElement : skillsNode) {
                        String skillName = skillElement.path("name").asText("").trim();
                        String category = skillElement.path("category").asText("TECHNICAL").trim();
                        addSkillToProfile(profile, skillName, category, existingSkillNames);
                    }
                }

                // Parse Projects
                JsonNode projectsNode = rootNode.path("projects");
                if (projectsNode.isArray()) {
                    for (JsonNode projElement : projectsNode) {
                        String title = projElement.path("title").asText("").trim();
                        String role = projElement.path("role").asText("").trim();
                        String desc = projElement.path("description").asText("").trim();

                        if (!title.isBlank() && !existingProjectTitles.contains(title.toLowerCase())) {
                            Project project = Project.builder()
                                    .id(UUID.randomUUID())
                                    .profile(profile)
                                    .title(title)
                                    .role(role)
                                    .description(desc)
                                    .build();
                            profile.getProjects().add(project);
                            existingProjectTitles.add(title.toLowerCase());
                        }
                    }
                }

                // Parse Experiences
                JsonNode expNode = rootNode.path("experiences");
                if (expNode.isArray() && (profile.getExperiences() == null || profile.getExperiences().isEmpty())) {
                    for (JsonNode elem : expNode) {
                        String company = elem.path("company").asText("").trim();
                        String role = elem.path("role").asText("").trim();
                        String desc = elem.path("description").asText("").trim();
                        String start = elem.path("startDate").asText("").trim();
                        String end = elem.path("endDate").asText("").trim();

                        if (!company.isBlank()) {
                            Experience exp = Experience.builder()
                                    .id(UUID.randomUUID())
                                    .profile(profile)
                                    .company(company)
                                    .role(role)
                                    .description(desc)
                                    .startDate(start)
                                    .endDate(end)
                                    .build();
                            profile.getExperiences().add(exp);
                        }
                    }
                }

                // Parse Educations
                JsonNode eduNode = rootNode.path("educations");
                if (eduNode.isArray() && (profile.getEducations() == null || profile.getEducations().isEmpty())) {
                    for (JsonNode elem : eduNode) {
                        String school = elem.path("school").asText("").trim();
                        String degree = elem.path("degree").asText("").trim();
                        String field = elem.path("fieldOfStudy").asText("").trim();
                        Double gpa = elem.has("gpa") && !elem.path("gpa").isNull() ? elem.path("gpa").asDouble() : null;

                        if (!school.isBlank()) {
                            Education edu = Education.builder()
                                    .id(UUID.randomUUID())
                                    .profile(profile)
                                    .school(school)
                                    .degree(degree)
                                    .fieldOfStudy(field)
                                    .gpa(gpa)
                                    .build();
                            profile.getEducations().add(edu);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Non-fatal error parsing resume with AI: {}", e.getMessage());
        }

        profileRepository.save(profile);
        log.info("Successfully extracted and synced {} skills and {} projects for user {}", 
                profile.getSkills().size(), profile.getProjects().size(), user.getId());
    }

    private void extractDeterministicSkills(CareerProfile profile, String text, Set<String> existingSkillNames) {
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("Hardware:") || trimmed.startsWith("Networking:") || 
                trimmed.startsWith("Operating Systems:") || trimmed.startsWith("Programming:") || 
                trimmed.startsWith("Backend & Web:") || trimmed.startsWith("Databases:") || 
                trimmed.startsWith("Tools:")) {
                
                String[] parts = trimmed.split(":", 2);
                if (parts.length == 2) {
                    String category = parts[0].trim();
                    String itemsStr = parts[1].trim();
                    String[] skillItems = itemsStr.split("[,;•|]");
                    for (String item : skillItems) {
                        String cleanSkill = item.replaceAll("[()0-9/]", " ").trim();
                        cleanSkill = item.trim();
                        if (!cleanSkill.isBlank() && cleanSkill.length() > 1 && cleanSkill.length() < 50) {
                            addSkillToProfile(profile, cleanSkill, category, existingSkillNames);
                        }
                    }
                }
            }
        }
    }

    private void extractDeterministicProjects(CareerProfile profile, String text, Set<String> existingProjectTitles) {
        String[] lines = text.split("\\r?\\n");
        boolean inProjectsSection = false;
        String currentTitle = null;
        String currentTech = null;
        StringBuilder currentDesc = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.equalsIgnoreCase("PROJECTS") || trimmed.equalsIgnoreCase("ACADEMIC PROJECTS") || trimmed.equalsIgnoreCase("TECHNICAL PROJECTS")) {
                inProjectsSection = true;
                continue;
            }
            if (inProjectsSection && (trimmed.equalsIgnoreCase("LEADERSHIP & ACHIEVEMENTS") || 
                                      trimmed.equalsIgnoreCase("CERTIFICATIONS") || 
                                      trimmed.equalsIgnoreCase("EDUCATION") || 
                                      trimmed.equalsIgnoreCase("EXPERIENCE") ||
                                      trimmed.equalsIgnoreCase("PROFESSIONAL EXPERIENCE"))) {
                // Save last pending project
                savePendingProject(profile, currentTitle, currentTech, currentDesc.toString(), existingProjectTitles);
                inProjectsSection = false;
                break;
            }

            if (inProjectsSection) {
                if (trimmed.contains("–") || trimmed.contains("-") || trimmed.startsWith("Prescription Reader") || trimmed.startsWith("EyeSonic") || trimmed.startsWith("Client Websites")) {
                    if (currentTitle != null) {
                        savePendingProject(profile, currentTitle, currentTech, currentDesc.toString(), existingProjectTitles);
                        currentDesc.setLength(0);
                    }
                    currentTitle = trimmed;
                    currentTech = "";
                } else if (trimmed.startsWith("•") || trimmed.startsWith("-") || trimmed.startsWith("*")) {
                    if (currentDesc.length() > 0) currentDesc.append(" ");
                    currentDesc.append(trimmed);
                } else if (!trimmed.isBlank()) {
                    if (currentTech != null && currentTech.isEmpty()) {
                        currentTech = trimmed;
                    } else {
                        if (currentDesc.length() > 0) currentDesc.append(" ");
                        currentDesc.append(trimmed);
                    }
                }
            }
        }

        if (inProjectsSection && currentTitle != null) {
            savePendingProject(profile, currentTitle, currentTech, currentDesc.toString(), existingProjectTitles);
        }
    }

    private void savePendingProject(CareerProfile profile, String title, String role, String desc, Set<String> existingProjectTitles) {
        if (title == null || title.isBlank()) return;
        String cleanTitle = title.trim();
        if (!existingProjectTitles.contains(cleanTitle.toLowerCase())) {
            Project project = Project.builder()
                    .id(UUID.randomUUID())
                    .profile(profile)
                    .title(cleanTitle)
                    .role(role != null ? role.trim() : "")
                    .description(desc != null ? desc.trim() : "")
                    .build();
            profile.getProjects().add(project);
            existingProjectTitles.add(cleanTitle.toLowerCase());
        }
    }

    private void addSkillToProfile(CareerProfile profile, String skillName, String category, Set<String> existingSkillNames) {
        if (skillName == null || skillName.isBlank()) return;
        String normalized = skillName.trim();
        if (!existingSkillNames.contains(normalized.toLowerCase())) {
            Skill skill = skillRepository.findByNameIgnoreCase(normalized)
                    .orElseGet(() -> skillRepository.save(Skill.builder()
                            .name(normalized)
                            .category(category)
                            .build()));

            ProfileSkill profileSkill = ProfileSkill.builder()
                    .id(new ProfileSkill.ProfileSkillId(profile.getId(), skill.getId()))
                    .profile(profile)
                    .skill(skill)
                    .selfAssessedLevel("PROFICIENT")
                    .build();

            profile.getSkills().add(profileSkill);
            existingSkillNames.add(normalized.toLowerCase());
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

package com.jobreadiness.copilot.resume.service;

import com.jobreadiness.copilot.common.exception.BadRequestException;
import com.jobreadiness.copilot.common.exception.ResourceNotFoundException;
import com.jobreadiness.copilot.resume.entity.Resume;
import com.jobreadiness.copilot.resume.entity.ResumeVersion;
import com.jobreadiness.copilot.resume.repository.ResumeRepository;
import com.jobreadiness.copilot.resume.repository.ResumeVersionRepository;
import com.jobreadiness.copilot.resume.util.DocumentTextExtractor;
import com.jobreadiness.copilot.storage.FileStorageService;
import com.jobreadiness.copilot.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository versionRepository;
    private final FileStorageService fileStorageService;
    private final DocumentTextExtractor textExtractor;

    public ResumeService(
            ResumeRepository resumeRepository,
            ResumeVersionRepository versionRepository,
            FileStorageService fileStorageService,
            DocumentTextExtractor textExtractor) {
        this.resumeRepository = resumeRepository;
        this.versionRepository = versionRepository;
        this.fileStorageService = fileStorageService;
        this.textExtractor = textExtractor;
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

        return resume;
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

package com.jobreadiness.copilot.resume.controller;

import com.jobreadiness.copilot.common.response.ApiResponse;
import com.jobreadiness.copilot.common.security.UserPrincipal;
import com.jobreadiness.copilot.resume.entity.Resume;
import com.jobreadiness.copilot.resume.service.ResumeService;
import com.jobreadiness.copilot.user.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Resume>> uploadResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) throws IOException {
        User user = User.builder().id(principal.getId()).email(principal.getEmail()).build();
        Resume resume = resumeService.uploadResume(user, file);
        return ResponseEntity.ok(ApiResponse.success(resume, "Resume uploaded and text extracted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Resume>>> getResumes(@AuthenticationPrincipal UserPrincipal principal) {
        List<Resume> resumes = resumeService.getResumes(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(resumes, "Resumes list retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Resume>> getResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        Resume resume = resumeService.getResume(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(resume, "Resume details retrieved successfully"));
    }
}

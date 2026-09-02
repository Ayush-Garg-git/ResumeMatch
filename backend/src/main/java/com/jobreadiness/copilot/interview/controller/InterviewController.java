package com.jobreadiness.copilot.interview.controller;

import com.jobreadiness.copilot.common.response.ApiResponse;
import com.jobreadiness.copilot.common.security.UserPrincipal;
import com.jobreadiness.copilot.interview.entity.InterviewQuestion;
import com.jobreadiness.copilot.interview.service.InterviewService;
import com.jobreadiness.copilot.user.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interview")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping("/prep/{jobId}")
    public ResponseEntity<ApiResponse<List<InterviewQuestion>>> generatePrepQuestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId) {
        User user = User.builder().id(principal.getId()).email(principal.getEmail()).build();
        List<InterviewQuestion> questions = interviewService.generateQuestionsForJob(user, jobId);
        return ResponseEntity.ok(ApiResponse.success(questions, "Mock interview questions generated successfully"));
    }

    @GetMapping("/prep/{jobId}")
    public ResponseEntity<ApiResponse<List<InterviewQuestion>>> getPrepQuestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId) {
        List<InterviewQuestion> questions = interviewService.getQuestionsForJob(jobId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(questions, "Mock interview questions list retrieved successfully"));
    }
}

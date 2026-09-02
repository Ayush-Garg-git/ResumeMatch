package com.jobreadiness.copilot.readiness.controller;

import com.jobreadiness.copilot.common.response.ApiResponse;
import com.jobreadiness.copilot.common.security.UserPrincipal;
import com.jobreadiness.copilot.readiness.entity.ReadinessAnalysis;
import com.jobreadiness.copilot.readiness.service.ReadinessService;
import com.jobreadiness.copilot.user.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/readiness")
public class ReadinessController {

    private final ReadinessService readinessService;

    public ReadinessController(ReadinessService readinessService) {
        this.readinessService = readinessService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReadinessAnalysis>> performAnalysis(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("jobId") UUID jobId) {
        User user = User.builder().id(principal.getId()).email(principal.getEmail()).build();
        ReadinessAnalysis analysis = readinessService.performReadinessAnalysis(user, jobId);
        return ResponseEntity.ok(ApiResponse.success(analysis, "Readiness analysis completed successfully"));
    }

    @PostMapping("/{jobId}")
    public ResponseEntity<ApiResponse<ReadinessAnalysis>> performAnalysisPath(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId) {
        User user = User.builder().id(principal.getId()).email(principal.getEmail()).build();
        ReadinessAnalysis analysis = readinessService.performReadinessAnalysis(user, jobId);
        return ResponseEntity.ok(ApiResponse.success(analysis, "Readiness analysis completed successfully"));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<ReadinessAnalysis>> getLatestAnalysis(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId) {
        ReadinessAnalysis analysis = readinessService.getLatestAnalysis(jobId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(analysis, "Readiness analysis retrieved successfully"));
    }
}

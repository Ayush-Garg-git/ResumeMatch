package com.jobreadiness.copilot.roadmap.controller;

import com.jobreadiness.copilot.common.response.ApiResponse;
import com.jobreadiness.copilot.common.security.UserPrincipal;
import com.jobreadiness.copilot.proof.dto.ProofSubmissionDto;
import com.jobreadiness.copilot.proof.entity.ProofSubmission;
import com.jobreadiness.copilot.roadmap.entity.LearningMission;
import com.jobreadiness.copilot.roadmap.service.RoadmapService;
import com.jobreadiness.copilot.user.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roadmap")
public class RoadmapController {

    private final RoadmapService roadmapService;

    public RoadmapController(RoadmapService roadmapService) {
        this.roadmapService = roadmapService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<List<LearningMission>>> generateRoadmap(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("jobId") UUID jobId) {
        User user = User.builder().id(principal.getId()).email(principal.getEmail()).build();
        List<LearningMission> missions = roadmapService.generateRoadmapForJob(user, jobId);
        return ResponseEntity.ok(ApiResponse.success(missions, "Learning roadmap missions generated successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LearningMission>>> getRoadmap(@AuthenticationPrincipal UserPrincipal principal) {
        List<LearningMission> missions = roadmapService.getRoadmap(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(missions, "Learning roadmap list retrieved successfully"));
    }

    @PostMapping("/mission/{missionId}/proof")
    public ResponseEntity<ApiResponse<ProofSubmission>> submitProof(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID missionId,
            @RequestBody ProofSubmissionDto submissionDto) {
        User user = User.builder().id(principal.getId()).email(principal.getEmail()).build();
        ProofSubmission submission = roadmapService.submitProof(user, missionId, submissionDto);
        return ResponseEntity.ok(ApiResponse.success(submission, "Proof of learning submitted successfully"));
    }
}

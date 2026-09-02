package com.jobreadiness.copilot.job.controller;

import com.jobreadiness.copilot.common.response.ApiResponse;
import com.jobreadiness.copilot.common.security.UserPrincipal;
import com.jobreadiness.copilot.job.dto.CreateJobRequest;
import com.jobreadiness.copilot.job.dto.JobDto;
import com.jobreadiness.copilot.job.service.JobService;
import com.jobreadiness.copilot.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JobDto>> createJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateJobRequest request) {
        User user = User.builder().id(principal.getId()).email(principal.getEmail()).build();
        JobDto jobDto = jobService.createJob(user, request);
        return ResponseEntity.ok(ApiResponse.success(jobDto, "Job description uploaded and analyzed successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobDto>>> getJobs(@AuthenticationPrincipal UserPrincipal principal) {
        List<JobDto> jobs = jobService.getJobs(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(jobs, "Jobs list retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobDto>> getJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        JobDto jobDto = jobService.getJob(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(jobDto, "Job details retrieved successfully"));
    }
}

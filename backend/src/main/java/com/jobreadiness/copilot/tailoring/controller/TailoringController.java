package com.jobreadiness.copilot.tailoring.controller;

import com.jobreadiness.copilot.claim.entity.Claim;
import com.jobreadiness.copilot.claim.repository.ClaimRepository;
import com.jobreadiness.copilot.common.response.ApiResponse;
import com.jobreadiness.copilot.common.security.UserPrincipal;
import com.jobreadiness.copilot.resume.entity.ResumeVersion;
import com.jobreadiness.copilot.tailoring.dto.StartSessionRequest;
import com.jobreadiness.copilot.tailoring.entity.TailoringSession;
import com.jobreadiness.copilot.tailoring.service.TailoringService;
import com.jobreadiness.copilot.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tailoring")
public class TailoringController {

    private final TailoringService tailoringService;
    private final ClaimRepository claimRepository;

    public TailoringController(TailoringService tailoringService, ClaimRepository claimRepository) {
        this.tailoringService = tailoringService;
        this.claimRepository = claimRepository;
    }

    @PostMapping("/session")
    public ResponseEntity<ApiResponse<TailoringSession>> startSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StartSessionRequest request) {
        User user = User.builder().id(principal.getId()).email(principal.getEmail()).build();
        TailoringSession session = tailoringService.startSession(user, request);
        return ResponseEntity.ok(ApiResponse.success(session, "Tailoring session initialized successfully"));
    }

    @GetMapping("/session/{id}/claims")
    public ResponseEntity<ApiResponse<List<Claim>>> getClaims(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        List<Claim> claims = claimRepository.findByTailoringSessionIdAndUserId(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(claims, "Claims list retrieved successfully"));
    }

    @PutMapping("/claims/{claimId}")
    public ResponseEntity<ApiResponse<Claim>> updateClaim(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID claimId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "proposedText", required = false) String proposedText) {
        User user = User.builder().id(principal.getId()).email(principal.getEmail()).build();
        Claim claim = tailoringService.updateClaim(user, claimId, status, proposedText);
        return ResponseEntity.ok(ApiResponse.success(claim, "Claim updated successfully"));
    }

    @PostMapping("/session/{id}/generate")
    public ResponseEntity<ApiResponse<ResumeVersion>> generateTailoredResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        User user = User.builder().id(principal.getId()).email(principal.getEmail()).build();
        ResumeVersion version = tailoringService.compileTailoredResume(user, id);
        return ResponseEntity.ok(ApiResponse.success(version, "Tailored resume version compiled successfully"));
    }
}

package com.jobreadiness.copilot.evidence.controller;

import com.jobreadiness.copilot.common.response.ApiResponse;
import com.jobreadiness.copilot.common.security.UserPrincipal;
import com.jobreadiness.copilot.evidence.dto.EvidenceDto;
import com.jobreadiness.copilot.evidence.entity.Evidence;
import com.jobreadiness.copilot.evidence.service.EvidenceService;
import com.jobreadiness.copilot.user.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @PostMapping("/discover")
    public ResponseEntity<ApiResponse<String>> discoverQuestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("jobId") UUID jobId) {
        String questionsJson = evidenceService.discoverEvidenceQuestions(jobId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(questionsJson, "Questions generated successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Evidence>> saveEvidence(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody EvidenceDto evidenceDto) {
        User user = User.builder().id(principal.getId()).email(principal.getEmail()).build();
        Evidence evidence = evidenceService.saveEvidence(user, evidenceDto);
        return ResponseEntity.ok(ApiResponse.success(evidence, "Evidence recorded and verified successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Evidence>>> getEvidenceBank(@AuthenticationPrincipal UserPrincipal principal) {
        List<Evidence> evidenceList = evidenceService.getEvidenceBank(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(evidenceList, "Evidence bank list retrieved successfully"));
    }
}

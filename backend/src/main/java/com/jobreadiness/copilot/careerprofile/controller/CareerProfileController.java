package com.jobreadiness.copilot.careerprofile.controller;

import com.jobreadiness.copilot.careerprofile.dto.CareerProfileDto;
import com.jobreadiness.copilot.careerprofile.service.CareerProfileService;
import com.jobreadiness.copilot.common.response.ApiResponse;
import com.jobreadiness.copilot.common.security.UserPrincipal;
import com.jobreadiness.copilot.user.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
public class CareerProfileController {

    private final CareerProfileService profileService;

    public CareerProfileController(CareerProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CareerProfileDto>> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        User user = User.builder().id(principal.getId()).email(principal.getEmail()).build();
        CareerProfileDto profile = profileService.getProfile(user);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile retrieved successfully"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<CareerProfileDto>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CareerProfileDto profileDto) {
        User user = User.builder().id(principal.getId()).email(principal.getEmail()).build();
        CareerProfileDto profile = profileService.updateProfile(user, profileDto);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile updated successfully"));
    }
}

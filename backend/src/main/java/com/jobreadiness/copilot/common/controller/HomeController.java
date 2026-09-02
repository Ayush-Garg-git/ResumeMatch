package com.jobreadiness.copilot.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping({"/", "/health", "/api/v1/health"})
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "ResumeMatch AI Copilot API",
            "version", "1.0.0",
            "timestamp", Instant.now().toString(),
            "endpoints", Map.of(
                "auth", "/api/v1/auth",
                "profile", "/api/v1/profile",
                "resumes", "/api/v1/resumes",
                "jobs", "/api/v1/jobs",
                "readiness", "/api/v1/readiness"
            )
        ));
    }
}

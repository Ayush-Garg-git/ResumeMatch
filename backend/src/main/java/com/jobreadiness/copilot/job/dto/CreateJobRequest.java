package com.jobreadiness.copilot.job.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateJobRequest {
    @NotBlank(message = "Job title is required")
    private String title;

    private String company;

    private String description;

    private String rawJdText;
}

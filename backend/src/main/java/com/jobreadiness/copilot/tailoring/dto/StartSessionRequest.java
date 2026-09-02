package com.jobreadiness.copilot.tailoring.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class StartSessionRequest {
    @NotNull(message = "Job ID is required")
    private UUID jobId;

    @NotNull(message = "Resume ID is required")
    private UUID resumeId;
}

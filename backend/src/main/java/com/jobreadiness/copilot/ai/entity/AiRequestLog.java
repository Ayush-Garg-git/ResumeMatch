package com.jobreadiness.copilot.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRequestLog {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String operation;

    @Column(nullable = false)
    private String model;

    @Column(name = "prompt_version")
    private String promptVersion;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(nullable = false)
    private String status; // SUCCESS, FAILED

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
    }
}

package com.jobreadiness.copilot.proof.entity;

import com.jobreadiness.copilot.roadmap.entity.LearningMission;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "proof_submissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProofSubmission {

    @Id
    private UUID id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_mission_id", nullable = false)
    private LearningMission learningMission;

    @Column(name = "proof_type", nullable = false)
    private String proofType; // GITHUB, DEPLOYMENT, SCREENSHOT, CODE, WRITTEN, CERTIFICATE

    @Column(name = "proof_url")
    private String proofUrl;

    @Column(name = "proof_content")
    private String proofContent;

    private String feedback;

    @Column(nullable = false)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }
}

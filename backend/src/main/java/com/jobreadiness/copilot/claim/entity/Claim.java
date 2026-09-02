package com.jobreadiness.copilot.claim.entity;

import com.jobreadiness.copilot.evidence.entity.Evidence;
import com.jobreadiness.copilot.skill.entity.Skill;
import com.jobreadiness.copilot.tailoring.entity.TailoringSession;
import com.jobreadiness.copilot.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "claims")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Claim {

    @Id
    private UUID id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tailoring_session_id", nullable = false)
    private TailoringSession tailoringSession;

    @Column(name = "original_text", nullable = false)
    private String originalText;

    @Column(name = "proposed_text", nullable = false)
    private String proposedText;

    @Column(nullable = false)
    private String status; // PENDING, APPROVED, REJECTED, EDITED

    private String confidence; // STRONG, MEDIUM, WEAK

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "claim_skills",
            joinColumns = @JoinColumn(name = "claim_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    @Builder.Default
    private List<Skill> skills = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "claim_evidences",
            joinColumns = @JoinColumn(name = "claim_id"),
            inverseJoinColumns = @JoinColumn(name = "evidence_id")
    )
    @Builder.Default
    private List<Evidence> evidences = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

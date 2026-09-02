package com.jobreadiness.copilot.readiness.entity;

import com.jobreadiness.copilot.skill.entity.Skill;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "skill_assessments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillAssessment {

    @Id
    private UUID id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "readiness_analysis_id", nullable = false)
    private ReadinessAnalysis readinessAnalysis;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false)
    private String classification; // DEMONSTRATED, HIDDEN_BUT_LIKELY, CONFIRMED_BUT_WEAK, GENUINE_GAP

    private String details;

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

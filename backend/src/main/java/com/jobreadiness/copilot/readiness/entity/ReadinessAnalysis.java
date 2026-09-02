package com.jobreadiness.copilot.readiness.entity;

import com.jobreadiness.copilot.job.entity.Job;
import com.jobreadiness.copilot.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "readiness_analyses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadinessAnalysis {

    @Id
    private UUID id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "eligibility_summary")
    private String eligibilitySummary;

    @Column(name = "technical_fit_summary")
    private String technicalFitSummary;

    @Column(name = "evidence_strength_summary")
    private String evidenceStrengthSummary;

    @Column(name = "presentation_summary")
    private String presentationSummary;

    @Column(nullable = false)
    private String recommendation;

    @OneToMany(mappedBy = "readinessAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SkillAssessment> skillAssessments = new ArrayList<>();

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

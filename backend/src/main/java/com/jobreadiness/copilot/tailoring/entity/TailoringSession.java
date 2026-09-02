package com.jobreadiness.copilot.tailoring.entity;

import com.jobreadiness.copilot.claim.entity.Claim;
import com.jobreadiness.copilot.job.entity.Job;
import com.jobreadiness.copilot.resume.entity.Resume;
import com.jobreadiness.copilot.resume.entity.ResumeVersion;
import com.jobreadiness.copilot.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tailoring_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TailoringSession {

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

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_resume_id", nullable = false)
    private Resume sourceResume;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tailored_resume_version_id")
    private ResumeVersion tailoredResumeVersion;

    @Column(nullable = false)
    private String status; // PENDING, COMPLETED, FAILED

    @OneToMany(mappedBy = "tailoringSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Claim> claims = new ArrayList<>();

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

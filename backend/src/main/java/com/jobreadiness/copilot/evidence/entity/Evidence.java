package com.jobreadiness.copilot.evidence.entity;

import com.jobreadiness.copilot.skill.entity.Skill;
import com.jobreadiness.copilot.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "evidence")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Evidence {

    @Id
    private UUID id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "evidence_type", nullable = false)
    private String evidenceType; // PROJECT, INTERNSHIP, COURSEWORK, CERTIFICATION, GITHUB, PORTFOLIO, STUDENT_RESPONSE, OTHER

    @Column(name = "source_type", nullable = false)
    private String sourceType; // PROJECT, EXPERIENCE, CERTIFICATION, NONE

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(nullable = false)
    private String confidence; // STRONG, MEDIUM, WEAK

    @Column(name = "student_confirmed", nullable = false)
    private Boolean studentConfirmed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        if (studentConfirmed == null) {
            studentConfirmed = false;
        }
    }
}

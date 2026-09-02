package com.jobreadiness.copilot.roadmap.entity;

import com.jobreadiness.copilot.skill.entity.Skill;
import com.jobreadiness.copilot.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "learning_missions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningMission {

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

    @Column(nullable = false)
    private String priority; // HIGH, MEDIUM, LOW

    private String explanation;

    @Column(name = "doc_links")
    private String docLinks;

    private String resources;

    @Column(name = "practice_questions")
    private String practiceQuestions;

    @Column(name = "mini_project_desc")
    private String miniProjectDesc;

    @Column(name = "deliverables_desc")
    private String deliverablesDesc;

    @Column(nullable = false)
    private String status; // EXPLORED, LEARNED, PRACTICED, BUILT, VERIFIED, EXPERIENCED

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
            status = "EXPLORED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

package com.jobreadiness.copilot.careerprofile.entity;

import com.jobreadiness.copilot.skill.entity.Skill;
import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "profile_skills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSkill {

    @EmbeddedId
    private ProfileSkillId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("profileId")
    @JoinColumn(name = "profile_id")
    private CareerProfile profile;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(name = "self_assessed_level")
    private String selfAssessedLevel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class ProfileSkillId implements Serializable {
        @Column(name = "profile_id")
        private UUID profileId;

        @Column(name = "skill_id")
        private UUID skillId;
    }
}

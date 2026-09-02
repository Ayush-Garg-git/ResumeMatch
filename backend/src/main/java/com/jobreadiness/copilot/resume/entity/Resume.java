package com.jobreadiness.copilot.resume.entity;

import com.jobreadiness.copilot.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resumes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resume {

    @Id
    private UUID id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String filename;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "raw_text")
    private String rawText;

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

package com.jobreadiness.copilot.careerprofile.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "certifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certification {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private CareerProfile profile;

    @Column(nullable = false)
    private String name;

    @Column(name = "issuing_organization")
    private String issuingOrganization;

    @Column(name = "issue_date")
    private String issueDate;

    @Column(name = "expiration_date")
    private String expirationDate;

    @Column(name = "credential_url")
    private String credentialUrl;

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

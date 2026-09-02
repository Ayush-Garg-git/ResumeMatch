package com.jobreadiness.copilot.careerprofile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificationDto {
    private UUID id;
    private String name;
    private String issuingOrganization;
    private String issueDate;
    private String expirationDate;
    private String credentialUrl;
}

package com.jobreadiness.copilot.proof.dto;

import lombok.Data;

@Data
public class ProofSubmissionDto {
    private String proofType; // GITHUB, DEPLOYMENT, SCREENSHOT, CODE, WRITTEN, CERTIFICATE
    private String proofUrl;
    private String proofContent;
}

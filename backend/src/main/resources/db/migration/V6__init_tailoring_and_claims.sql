CREATE TABLE tailoring_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    source_resume_id UUID NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    tailored_resume_version_id UUID REFERENCES resume_versions(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL, -- PENDING, COMPLETED, FAILED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE claims (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tailoring_session_id UUID NOT NULL REFERENCES tailoring_sessions(id) ON DELETE CASCADE,
    original_text TEXT NOT NULL,
    proposed_text TEXT NOT NULL,
    status VARCHAR(50) NOT NULL, -- PENDING, APPROVED, REJECTED, EDITED
    confidence VARCHAR(50), -- STRONG, MEDIUM, WEAK
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE claim_skills (
    claim_id UUID NOT NULL REFERENCES claims(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (claim_id, skill_id)
);

CREATE TABLE claim_evidences (
    claim_id UUID NOT NULL REFERENCES claims(id) ON DELETE CASCADE,
    evidence_id UUID NOT NULL REFERENCES evidence(id) ON DELETE CASCADE,
    PRIMARY KEY (claim_id, evidence_id)
);

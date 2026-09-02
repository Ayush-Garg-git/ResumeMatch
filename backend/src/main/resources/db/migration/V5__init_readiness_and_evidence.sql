CREATE TABLE readiness_analyses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    eligibility_summary TEXT,
    technical_fit_summary TEXT,
    evidence_strength_summary TEXT,
    presentation_summary TEXT,
    recommendation VARCHAR(100) NOT NULL, -- APPLY_NOW, APPLY_AFTER_TAILORING, STRETCH_APPLICATION, BUILD_BEFORE_APPLYING
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE skill_assessments (
    id UUID PRIMARY KEY,
    readiness_analysis_id UUID NOT NULL REFERENCES readiness_analyses(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    classification VARCHAR(100) NOT NULL, -- DEMONSTRATED, HIDDEN_BUT_LIKELY, CONFIRMED_BUT_WEAK, GENUINE_GAP
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE evidence (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    evidence_type VARCHAR(100) NOT NULL, -- PROJECT, INTERNSHIP, COURSEWORK, CERTIFICATION, GITHUB, PORTFOLIO, STUDENT_RESPONSE, OTHER
    source_type VARCHAR(100) NOT NULL, -- PROJECT, EXPERIENCE, CERTIFICATION, NONE
    source_id UUID, -- Reference to target table primary key (e.g. project_id, experience_id)
    confidence VARCHAR(50) NOT NULL, -- STRONG, MEDIUM, WEAK
    student_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

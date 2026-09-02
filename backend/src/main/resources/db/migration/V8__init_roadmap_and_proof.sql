CREATE TABLE learning_missions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    priority VARCHAR(50) NOT NULL, -- HIGH, MEDIUM, LOW
    explanation TEXT,
    doc_links TEXT,
    resources TEXT,
    practice_questions TEXT,
    mini_project_desc TEXT,
    deliverables_desc TEXT,
    status VARCHAR(50) NOT NULL, -- EXPLORED, LEARNED, PRACTICED, BUILT, VERIFIED, EXPERIENCED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE proof_submissions (
    id UUID PRIMARY KEY,
    learning_mission_id UUID NOT NULL REFERENCES learning_missions(id) ON DELETE CASCADE,
    proof_type VARCHAR(100) NOT NULL, -- GITHUB, DEPLOYMENT, SCREENSHOT, CODE, WRITTEN, CERTIFICATE
    proof_url VARCHAR(255),
    proof_content TEXT,
    feedback TEXT,
    status VARCHAR(50) NOT NULL, -- PENDING, APPROVED, REJECTED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

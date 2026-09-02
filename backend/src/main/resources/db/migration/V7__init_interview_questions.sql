CREATE TABLE interview_questions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    claim_id UUID REFERENCES claims(id) ON DELETE SET NULL,
    question TEXT NOT NULL,
    suggested_answer_guideline TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

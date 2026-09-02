CREATE TABLE ai_requests (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    operation VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50),
    input_tokens INT,
    output_tokens INT,
    latency_ms BIGINT,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

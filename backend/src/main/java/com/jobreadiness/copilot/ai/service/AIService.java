package com.jobreadiness.copilot.ai.service;

import java.util.UUID;

public interface AIService {
    /**
     * Sends a request to the configured AI provider, logs token usage and latency.
     *
     * @param prompt The user prompt.
     * @param systemInstruction The system instructions (e.g., persona, output schema).
     * @param operation The name of the operation (for auditing/logs).
     * @param userId The ID of the user requesting the operation (nullable).
     * @return The text content returned by the AI provider (typically JSON).
     */
    String generateContent(String prompt, String systemInstruction, String operation, UUID userId);
}

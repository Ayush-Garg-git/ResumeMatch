package com.jobreadiness.copilot.ai.repository;

import com.jobreadiness.copilot.ai.entity.AiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AiRequestLogRepository extends JpaRepository<AiRequestLog, UUID> {
}

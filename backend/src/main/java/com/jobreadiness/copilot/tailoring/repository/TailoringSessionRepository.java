package com.jobreadiness.copilot.tailoring.repository;

import com.jobreadiness.copilot.tailoring.entity.TailoringSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TailoringSessionRepository extends JpaRepository<TailoringSession, UUID> {
    List<TailoringSession> findByUserId(UUID userId);
    Optional<TailoringSession> findByIdAndUserId(UUID id, UUID userId);
}

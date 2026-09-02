package com.jobreadiness.copilot.claim.repository;

import com.jobreadiness.copilot.claim.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {
    List<Claim> findByTailoringSessionIdAndUserId(UUID sessionId, UUID userId);
    Optional<Claim> findByIdAndUserId(UUID id, UUID userId);
}

package com.jobreadiness.copilot.careerprofile.repository;

import com.jobreadiness.copilot.careerprofile.entity.CareerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CareerProfileRepository extends JpaRepository<CareerProfile, UUID> {
    Optional<CareerProfile> findByUserId(UUID userId);
}

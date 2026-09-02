package com.jobreadiness.copilot.interview.repository;

import com.jobreadiness.copilot.interview.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, UUID> {
    List<InterviewQuestion> findByJobIdAndUserId(UUID jobId, UUID userId);
}

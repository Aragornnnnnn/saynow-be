package com.saynow.feedback.repository;

import com.saynow.feedback.domain.SessionFeedback;
import com.saynow.practice.domain.PracticeSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionFeedbackRepository extends JpaRepository<SessionFeedback, Long> {

    Optional<SessionFeedback> findBySession(PracticeSession session);

    boolean existsBySession(PracticeSession session);
}

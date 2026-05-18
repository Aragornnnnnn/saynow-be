package com.saynow.feedback.infrastructure;

import com.saynow.feedback.domain.SessionFeedback;
import com.saynow.session.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionFeedbackRepository extends JpaRepository<SessionFeedback, Long> {

    Optional<SessionFeedback> findBySession(Session session);

    boolean existsBySession(Session session);
}

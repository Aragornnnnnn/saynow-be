package com.saynow.feedback.infrastructure;

import com.saynow.feedback.domain.SessionFeedback;
import com.saynow.feedback.domain.TurnFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TurnFeedbackRepository extends JpaRepository<TurnFeedback, Long> {

    List<TurnFeedback> findBySessionFeedbackOrderByTurnSequenceAsc(SessionFeedback sessionFeedback);
}

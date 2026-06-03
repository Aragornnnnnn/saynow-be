package com.saynow.feedback.infrastructure;

import com.saynow.feedback.domain.SessionFeedback;
import com.saynow.feedback.domain.TurnFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TurnFeedbackRepository extends JpaRepository<TurnFeedback, Long> {

    @Query("""
            select feedback
            from TurnFeedback feedback
            join fetch feedback.turn turn
            where feedback.sessionFeedback = :sessionFeedback
            order by turn.sequence asc
            """)
    List<TurnFeedback> findBySessionFeedbackWithTurnOrderByTurnSequenceAsc(SessionFeedback sessionFeedback);
}

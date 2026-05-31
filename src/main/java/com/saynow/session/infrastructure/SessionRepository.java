// 시나리오 진행 세션을 조회하고 저장하는 JPA 저장소
package com.saynow.session.infrastructure;

import com.saynow.session.domain.Session;
import com.saynow.session.domain.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {

    @Query(value = """
            select
                s.id as sessionId,
                s.user_id as userId,
                s.scenario_id as scenarioId,
                s.status as sessionStatus,
                s.remaining_hearts as remainingHearts,
                sc.title as scenarioTitle,
                sc.ai_role as scenarioAiRole,
                sc.situation as scenarioSituation,
                sc.goal as scenarioGoal,
                t.id as currentTurnId,
                t.sequence as currentTurnSequence,
                t.ai_question as currentTurnAiQuestion,
                t.next_question_target_slot_name as currentTurnTargetSlotName,
                sss.slot_name as slotName,
                sss.is_fulfilled as slotFulfilled,
                ss.description as slotDescription,
                ss.evidence_policy as slotEvidencePolicy
            from sessions s
            join scenarios sc on sc.id = s.scenario_id
            left join session_turns t on t.id = (
                select t2.id
                from session_turns t2
                where t2.session_id = s.id
                  and t2.user_utterance is null
                order by t2.sequence asc
                limit 1
            )
            left join session_slot_statuses sss on sss.session_id = s.id
            left join scenario_slots ss on ss.scenario_id = sc.id and ss.name = sss.slot_name
            where s.id = :sessionId
              and s.user_id = :userId
            order by sss.id asc
            """, nativeQuery = true)
    List<SubmitUtteranceContextRow> findSubmitUtteranceContextRows(Long userId, Long sessionId);

    @Modifying
    @Query("""
            update Session s
            set s.remainingHearts = :remainingHearts
            where s.id = :sessionId
            """)
    void updateRemainingHearts(Long sessionId, int remainingHearts);

    @Modifying
    @Query("""
            update Session s
            set s.remainingHearts = :remainingHearts,
                s.status = :status,
                s.completedAt = :completedAt
            where s.id = :sessionId
            """)
    void updateCompletion(Long sessionId, int remainingHearts, SessionStatus status, LocalDateTime completedAt);

    interface SubmitUtteranceContextRow {

        Long getSessionId();

        Long getUserId();

        Long getScenarioId();

        String getSessionStatus();

        int getRemainingHearts();

        String getScenarioTitle();

        String getScenarioAiRole();

        String getScenarioSituation();

        String getScenarioGoal();

        Long getCurrentTurnId();

        Integer getCurrentTurnSequence();

        String getCurrentTurnAiQuestion();

        String getCurrentTurnTargetSlotName();

        String getSlotName();

        Boolean getSlotFulfilled();

        String getSlotDescription();

        String getSlotEvidencePolicy();
    }
}

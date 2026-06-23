// 세션 턴을 조회하고 저장하는 JPA 저장소
package com.saynow.session.infrastructure;

import com.saynow.session.domain.Session;
import com.saynow.session.domain.SessionStatus;
import com.saynow.session.domain.SessionTurn;
import com.saynow.session.domain.InnerThoughtType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SessionTurnRepository extends JpaRepository<SessionTurn, Long> {

    long countBySession(Session session);

    List<SessionTurn> findBySessionOrderBySequenceAsc(Session session);

    Optional<SessionTurn> findFirstBySessionAndUserUtteranceIsNullAndSequenceLessThanEqualOrderBySequenceAsc(
            Session session,
            int sequence
    );

    long countBySessionAndUserUtteranceIsNotNull(Session session);

    @Modifying
    @Query("""
            update SessionTurn turn
            set turn.userUtterance = :userUtterance,
                turn.innerThought = :innerThought,
                turn.innerThoughtType = :innerThoughtType,
                turn.answeredAt = CURRENT_TIMESTAMP
            where turn.id = :turnId
              and turn.session.id = :sessionId
              and turn.session.user.id = :userId
              and turn.session.status = :sessionStatus
              and turn.userUtterance is null
            """)
    int updateUserUtteranceIfPending(
            Long turnId,
            Long sessionId,
            Long userId,
            SessionStatus sessionStatus,
            String userUtterance,
            String innerThought,
            InnerThoughtType innerThoughtType
    );

    void deleteBySession(Session session);
}

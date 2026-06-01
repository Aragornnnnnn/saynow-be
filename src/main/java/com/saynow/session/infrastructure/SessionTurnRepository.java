// 세션 턴을 조회하고 저장하는 JPA 저장소
package com.saynow.session.infrastructure;

import com.saynow.session.domain.Session;
import com.saynow.session.domain.SessionTurn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SessionTurnRepository extends JpaRepository<SessionTurn, Long> {

    long countBySession(Session session);

    List<SessionTurn> findBySessionOrderBySequenceAsc(Session session);

    List<SessionTurn> findBySessionAndUserUtteranceIsNullOrderBySequenceAsc(Session session);

    @Modifying
    @Query("""
            update SessionTurn turn
            set turn.userUtterance = :userUtterance
            where turn.id = :turnId
              and turn.userUtterance is null
            """)
    int updateUserUtteranceIfPending(Long turnId, String userUtterance);

    void deleteBySession(Session session);
}

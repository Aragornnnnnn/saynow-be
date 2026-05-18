// 세션 턴을 조회하고 저장하는 JPA 저장소
package com.saynow.session.infrastructure;

import com.saynow.session.domain.Session;
import com.saynow.session.domain.SessionTurn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionTurnRepository extends JpaRepository<SessionTurn, Long> {

    long countBySession(Session session);

    List<SessionTurn> findBySessionOrderBySequenceAsc(Session session);

    List<SessionTurn> findBySessionAndUserUtteranceIsNullOrderBySequenceAsc(Session session);

    void deleteBySession(Session session);
}

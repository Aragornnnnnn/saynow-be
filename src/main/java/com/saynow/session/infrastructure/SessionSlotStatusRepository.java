// 세션 슬롯 충족 상태를 조회하고 저장하는 JPA 저장소
package com.saynow.session.infrastructure;

import com.saynow.session.domain.Session;
import com.saynow.session.domain.SessionSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionSlotStatusRepository extends JpaRepository<SessionSlotStatus, Long> {

    List<SessionSlotStatus> findBySessionOrderByIdAsc(Session session);

    void deleteBySession(Session session);
}

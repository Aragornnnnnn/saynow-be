// 세션 슬롯 충족 상태를 조회하고 저장하는 JPA 저장소
package com.saynow.session.infrastructure;

import com.saynow.session.domain.Session;
import com.saynow.session.domain.SessionSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface SessionSlotStatusRepository extends JpaRepository<SessionSlotStatus, Long> {

    List<SessionSlotStatus> findBySessionOrderByIdAsc(Session session);

    @Modifying
    @Query("""
            update SessionSlotStatus status
            set status.fulfilled = true
            where status.session.id = :sessionId
              and status.slotName in :slotNames
            """)
    void fulfillBySessionIdAndSlotNameIn(Long sessionId, Collection<String> slotNames);

    void deleteBySession(Session session);
}

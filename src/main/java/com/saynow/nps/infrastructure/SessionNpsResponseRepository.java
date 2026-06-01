// 세션 NPS 평가 저장 여부를 조회하고 저장하는 JPA 저장소
package com.saynow.nps.infrastructure;

import com.saynow.nps.domain.SessionNpsResponse;
import com.saynow.session.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionNpsResponseRepository extends JpaRepository<SessionNpsResponse, Long> {

    boolean existsBySession(Session session);
}

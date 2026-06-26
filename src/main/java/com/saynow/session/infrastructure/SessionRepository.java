// 시나리오 진행 세션을 조회하고 저장하는 JPA 저장소
package com.saynow.session.infrastructure;

import com.saynow.session.domain.Session;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    @EntityGraph(attributePaths = "scenario")
    Optional<Session> findByIdAndUserId(Long id, Long userId);
}

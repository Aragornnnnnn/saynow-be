// 사용자별 시나리오 진행 상태를 조회하고 저장하는 JPA 저장소
package com.saynow.scenario.infrastructure;

import com.saynow.auth.domain.User;
import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.UserScenarioProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserScenarioProgressRepository extends JpaRepository<UserScenarioProgress, Long> {

    List<UserScenarioProgress> findByUserIdAndScenarioIdIn(Long userId, Collection<Long> scenarioIds);

    Optional<UserScenarioProgress> findByUserAndScenario(User user, Scenario scenario);
}

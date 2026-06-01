// 시나리오별 고정 질문을 순서 기준으로 조회하는 저장소
package com.saynow.scenario.infrastructure;

import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.ScenarioQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScenarioQuestionRepository extends JpaRepository<ScenarioQuestion, Long> {

    List<ScenarioQuestion> findByScenarioInOrderByScenarioIdAscSequenceAsc(Collection<Scenario> scenarios);

    List<ScenarioQuestion> findByScenarioOrderBySequenceAsc(Scenario scenario);

    Optional<ScenarioQuestion> findByScenarioAndSequence(Scenario scenario, int sequence);
}

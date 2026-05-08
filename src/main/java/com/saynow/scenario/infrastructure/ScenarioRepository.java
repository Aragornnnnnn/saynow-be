package com.saynow.scenario.infrastructure;

import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.ScenarioCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    List<Scenario> findByCategoryOrderByIdAsc(ScenarioCategory category);

    Optional<Scenario> findByScenarioKey(String scenarioKey);
}

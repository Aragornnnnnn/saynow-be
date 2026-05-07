package com.saynow.scenario.repository;

import com.saynow.scenario.domain.ContentStatus;
import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.ScenarioCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    List<Scenario> findByCategoryAndStatusOrderBySortOrderAsc(ScenarioCategory category, ContentStatus status);

    Optional<Scenario> findByScenarioKeyAndStatus(String scenarioKey, ContentStatus status);
}

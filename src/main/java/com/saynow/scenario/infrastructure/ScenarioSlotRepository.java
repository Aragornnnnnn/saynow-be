package com.saynow.scenario.infrastructure;

import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.ScenarioSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScenarioSlotRepository extends JpaRepository<ScenarioSlot, Long> {

    List<ScenarioSlot> findByScenarioOrderBySlotOrderAsc(Scenario scenario);
}

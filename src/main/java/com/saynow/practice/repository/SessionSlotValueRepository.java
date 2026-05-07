package com.saynow.practice.repository;

import com.saynow.practice.domain.PracticeSession;
import com.saynow.practice.domain.SessionSlotValue;
import com.saynow.scenario.domain.ScenarioSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionSlotValueRepository extends JpaRepository<SessionSlotValue, Long> {

    boolean existsBySessionAndScenarioSlot(PracticeSession session, ScenarioSlot scenarioSlot);

    List<SessionSlotValue> findBySession(PracticeSession session);
}

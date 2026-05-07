package com.saynow.practice.infrastructure.ai;

import com.saynow.scenario.domain.ScenarioSlot;

import java.util.List;
import java.util.Set;

public record AiTurnEvaluationRequest(
        String transcript,
        List<ScenarioSlot> scenarioSlots,
        Set<String> currentFilledSlotKeys,
        long followUpCount,
        int maxFollowUpCount
) {
}

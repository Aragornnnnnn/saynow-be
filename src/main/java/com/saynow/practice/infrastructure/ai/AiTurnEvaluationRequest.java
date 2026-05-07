package com.saynow.practice.infrastructure.ai;

import com.saynow.practice.domain.InputType;
import com.saynow.scenario.domain.ScenarioSlot;

import java.util.List;
import java.util.Set;

public record AiTurnEvaluationRequest(
        String audioFilename,
        String audioContentType,
        byte[] audioContent,
        InputType inputType,
        Integer speechStartedAfterMs,
        Integer recordingDurationMs,
        List<ScenarioSlot> scenarioSlots,
        Set<String> currentFilledSlotKeys,
        long followUpCount,
        int maxFollowUpCount
) {
}

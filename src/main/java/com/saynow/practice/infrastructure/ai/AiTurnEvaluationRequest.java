package com.saynow.practice.infrastructure.ai;

import com.saynow.practice.domain.InputType;
import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.ScenarioSlot;

import java.util.List;
import java.util.Map;

public record AiTurnEvaluationRequest(
        String sessionId,
        Scenario scenario,
        String currentQuestionText,
        String currentQuestionTtsUrl,
        String audioFilename,
        String audioContentType,
        byte[] audioContent,
        InputType inputType,
        Integer speechStartedAfterMs,
        Integer recordingDurationMs,
        List<ScenarioSlot> scenarioSlots,
        Map<String, String> currentFilledSlots,
        long followUpCount,
        int maxFollowUpCount
) {
}

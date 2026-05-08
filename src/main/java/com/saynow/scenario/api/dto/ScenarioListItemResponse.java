package com.saynow.scenario.api.dto;

import com.saynow.scenario.domain.ScenarioDifficulty;

public record ScenarioListItemResponse(
        String scenarioId,
        String title,
        ScenarioDifficulty difficulty,
        String successGoal,
        String thumbnailUrl
) {
}

package com.saynow.scenario.api.dto;

public record ScenarioSummaryResponse(
        String scenarioId,
        String categoryId,
        String title,
        String difficulty,
        String successGoal,
        String thumbnailUrl
) {
}

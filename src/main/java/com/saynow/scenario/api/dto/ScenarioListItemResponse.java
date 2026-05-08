package com.saynow.scenario.api.dto;

public record ScenarioListItemResponse(
        String scenarioId,
        String title,
        String difficulty,
        String successGoal,
        String thumbnailUrl
) {
}

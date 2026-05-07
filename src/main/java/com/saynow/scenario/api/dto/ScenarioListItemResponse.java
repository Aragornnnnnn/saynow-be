package com.saynow.scenario.api.dto;

public record ScenarioListItemResponse(
        String scenarioId,
        String title,
        String successGoal,
        String thumbnailUrl,
        int sortOrder
) {
}

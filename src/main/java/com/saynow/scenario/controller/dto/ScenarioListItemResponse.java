package com.saynow.scenario.controller.dto;

public record ScenarioListItemResponse(
        String scenarioId,
        String title,
        String successGoal,
        String thumbnailUrl,
        int sortOrder
) {
}

package com.saynow.scenario.controller.dto;

public record ScenarioDetailResponse(
        String scenarioId,
        String categoryId,
        String title,
        String situationDescription,
        String successGoal,
        String openingBabsaeText,
        String openingTtsUrl,
        int maxFollowUpCount
) {
}

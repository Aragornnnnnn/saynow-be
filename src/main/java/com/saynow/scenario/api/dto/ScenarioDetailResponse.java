package com.saynow.scenario.api.dto;

public record ScenarioDetailResponse(
        String scenarioId,
        String categoryId,
        String title,
        String difficulty,
        String situationDescription,
        String successGoal,
        String openingBabsaeText,
        String openingTtsUrl,
        int maxFollowUpCount,
        String thumbnailUrl
) {
}

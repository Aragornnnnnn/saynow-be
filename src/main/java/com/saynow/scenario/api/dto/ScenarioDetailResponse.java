package com.saynow.scenario.api.dto;

import com.saynow.scenario.domain.ScenarioDifficulty;

public record ScenarioDetailResponse(
        String scenarioId,
        String categoryId,
        String title,
        ScenarioDifficulty difficulty,
        String situationDescription,
        String successGoal,
        String openingBabsaeText,
        String openingTtsUrl,
        int maxFollowUpCount
) {
}

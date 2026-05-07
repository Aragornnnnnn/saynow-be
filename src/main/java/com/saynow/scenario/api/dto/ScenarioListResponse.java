package com.saynow.scenario.api.dto;

import java.util.List;

public record ScenarioListResponse(
        String categoryId,
        List<ScenarioListItemResponse> scenarios
) {
}

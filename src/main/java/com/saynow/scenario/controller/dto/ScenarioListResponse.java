package com.saynow.scenario.controller.dto;

import java.util.List;

public record ScenarioListResponse(
        String categoryId,
        List<ScenarioListItemResponse> scenarios
) {
}

package com.saynow.scenario.api.dto;

import java.util.List;

public record ScenarioSummaryListResponse(
        List<ScenarioSummaryResponse> scenarios
) {
}

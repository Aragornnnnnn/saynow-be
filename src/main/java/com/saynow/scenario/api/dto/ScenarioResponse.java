// 홈 화면에 노출할 시나리오 카드 상태를 반환하는 DTO
package com.saynow.scenario.api.dto;

public record ScenarioResponse(
        Long scenarioId,
        int displayOrder,
        String scenarioTitle,
        String scenarioGoal,
        String scenarioSituation,
        String scenarioEmoji,
        boolean cleared,
        boolean locked,
        String lockReason
) {
}

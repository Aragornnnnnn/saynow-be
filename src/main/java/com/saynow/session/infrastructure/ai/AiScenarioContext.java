// AI 요청에 공통으로 전달하는 시나리오 요약 DTO
package com.saynow.session.infrastructure.ai;

public record AiScenarioContext(
        Long scenarioId,
        String title,
        String briefing,
        String conversationGoal
) {
}

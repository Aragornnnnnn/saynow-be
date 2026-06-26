// AI 다음 질문 생성 요청에 전달하는 상대 역할 포함 시나리오 컨텍스트 DTO
package com.saynow.session.infrastructure.ai;

public record AiNextQuestionScenarioContext(
        Long scenarioId,
        String title,
        String briefing,
        String conversationGoal,
        String counterpartRole,
        ServiceAudience serviceAudience
) {
}

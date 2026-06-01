// 발화 하나에 대한 턴별 피드백 생성을 AI 서버에 요청하는 DTO
package com.saynow.session.infrastructure.ai;

public record AiTurnFeedbackRequest(
        Long sessionId,
        Long turnId,
        int sequence,
        AiScenarioContext scenario,
        AiTurnContext turn
) {
}

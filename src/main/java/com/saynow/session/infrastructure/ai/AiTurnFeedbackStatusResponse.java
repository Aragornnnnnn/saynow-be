// AI 서버가 턴별 피드백 캐시 요청 접수 상태를 반환하는 DTO
package com.saynow.session.infrastructure.ai;

public record AiTurnFeedbackStatusResponse(
        Long sessionId,
        Long turnId,
        TurnFeedbackStatus feedbackStatus
) {
}

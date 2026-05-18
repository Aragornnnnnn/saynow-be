// AI 서버가 생성한 턴별 피드백 응답 DTO
package com.saynow.session.infrastructure.ai;

public record AiTurnFeedbackResponse(
        Long turnId,
        boolean feedbackRequired,
        String nativeUnderstanding,
        String nativeLanguageInterpretation,
        String betterExpression
) {
}

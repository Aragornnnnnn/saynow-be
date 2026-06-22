// AI 서버가 반환한 캐시 기반 턴별 피드백 DTO
package com.saynow.session.infrastructure.ai;

public record AiSessionTurnFeedbackResponse(
        Long turnId,
        FeedbackType feedbackType,
        String koreanAnalogy,
        String positiveFeedback,
        String feedbackDetail,
        String correctionExpression,
        String correctionReason,
        String benchmarkMessage
) {
}

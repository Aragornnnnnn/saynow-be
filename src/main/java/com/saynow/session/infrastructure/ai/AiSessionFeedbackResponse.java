// AI 서버가 반환한 세션 최종 피드백과 턴별 피드백 DTO
package com.saynow.session.infrastructure.ai;

import java.util.List;

public record AiSessionFeedbackResponse(
        Long sessionId,
        int nativeScore,
        String nativeLevelLabel,
        String summary,
        List<AiSessionTurnFeedbackResponse> turnFeedbacks
) {
}

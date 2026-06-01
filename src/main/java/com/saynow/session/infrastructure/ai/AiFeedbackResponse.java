// AI 서버가 생성한 세션 최종 피드백 응답 DTO
package com.saynow.session.infrastructure.ai;

import java.util.List;

public record AiFeedbackResponse(
        int comprehensionScore,
        String feedbackSummary,
        List<AiTurnFeedbackResponse> turnFeedbacks
) {
}

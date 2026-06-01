// 세션 최종 피드백 화면에 필요한 전체 응답 DTO
package com.saynow.feedback.api.dto;

import java.util.List;

public record FeedbackResponse(
        Long sessionId,
        int nativeScore,
        String nativeLevelLabel,
        String summary,
        List<TurnFeedbackResponse> turnFeedbacks
) {
}

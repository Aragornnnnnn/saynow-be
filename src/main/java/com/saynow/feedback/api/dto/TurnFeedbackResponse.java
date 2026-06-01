// 최종 피드백 화면의 턴별 품질 피드백 응답 DTO
package com.saynow.feedback.api.dto;

import com.saynow.session.infrastructure.ai.FeedbackType;

public record TurnFeedbackResponse(
        Long turnId,
        int sequence,
        String aiQuestion,
        String translatedQuestion,
        String userUtterance,
        FeedbackType feedbackType,
        String koreanAnalogy,
        String correctionPoint,
        String correctionReason,
        String plusOneExpression,
        String praiseSummary,
        String praiseReason
) {
}

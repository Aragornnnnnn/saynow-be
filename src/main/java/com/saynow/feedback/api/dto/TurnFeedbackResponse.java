// 채팅형 피드백 화면의 턴별 응답 DTO
package com.saynow.feedback.api.dto;

public record TurnFeedbackResponse(
        Long turnId,
        int sequence,
        String originalQuestion,
        String translatedQuestion,
        String userUtterance,
        boolean feedbackRequired,
        String nativeUnderstanding,
        String nativeLanguageInterpretation,
        String betterExpression
) {
}

// 사용자 발화 처리 후 다음 질문 또는 피드백 가능 상태를 반환하는 DTO
package com.saynow.session.api.dto;

public record UserUtteranceResponse(
        Long sessionId,
        String originalQuestion,
        String translatedQuestion,
        int remainingHearts,
        boolean feedbackAvailable
) {
}

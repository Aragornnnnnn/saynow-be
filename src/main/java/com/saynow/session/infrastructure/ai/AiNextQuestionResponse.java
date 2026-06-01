// AI 서버가 생성한 맞장구 포함 다음 질문 응답 DTO
package com.saynow.session.infrastructure.ai;

public record AiNextQuestionResponse(
        String aiQuestion,
        String translatedQuestion
) {
}

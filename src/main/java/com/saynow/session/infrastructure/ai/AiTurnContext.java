// AI 요청에 전달하는 현재 턴 질문과 사용자 발화 DTO
package com.saynow.session.infrastructure.ai;

public record AiTurnContext(
        String aiQuestion,
        String translatedQuestion,
        String userUtterance
) {
}

// AI가 자연스럽게 이어 붙여야 하는 다음 고정 질문 DTO
package com.saynow.session.infrastructure.ai;

public record AiFixedQuestion(
        Long questionId,
        int sequence,
        String questionEn,
        String questionKo
) {
}

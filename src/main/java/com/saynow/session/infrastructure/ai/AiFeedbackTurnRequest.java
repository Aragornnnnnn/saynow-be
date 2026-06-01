// AI 최종 피드백 요청에 포함할 턴 단위 발화 DTO
package com.saynow.session.infrastructure.ai;

public record AiFeedbackTurnRequest(
        Long turnId,
        String originalQuestion,
        String userUtterance
) {
}

// 새 시나리오 세션 시작 결과를 반환하는 DTO
package com.saynow.session.api.dto;

public record SessionStartResponse(
        Long sessionId,
        Long scenarioId,
        int totalQuestionCount,
        SessionTurnResponse currentTurn,
        SessionProgressResponse progress
) {
}

// 세션 진행 화면에 노출할 턴 질문 DTO
package com.saynow.session.api.dto;

public record SessionTurnResponse(
        Long turnId,
        int sequence,
        String aiQuestion,
        String translatedQuestion
) {
}

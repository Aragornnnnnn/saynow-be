// 시나리오 목록에서 빠른 렌더링용 첫 질문 미리보기를 반환하는 DTO
package com.saynow.scenario.api.dto;

public record FirstQuestionPreviewResponse(
        Long questionId,
        String aiQuestion,
        String translatedQuestion
) {
}

// 세션 중 영어 학습 가이드 질문을 받는 요청 DTO
package com.saynow.session.api.dto;

import jakarta.validation.constraints.NotBlank;

public record GuideQuestionRequest(
        @NotBlank String question
) {
}

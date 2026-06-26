// 세션 질문 진행률을 반환하는 DTO
package com.saynow.session.api.dto;

public record SessionProgressResponse(
        int currentSequence,
        int totalQuestionCount,
        boolean completed
) {
}

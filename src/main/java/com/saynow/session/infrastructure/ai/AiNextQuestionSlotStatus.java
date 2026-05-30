// 꼬리 질문 생성 요청에 포함할 슬롯 상태와 근거 검증 정책 DTO
package com.saynow.session.infrastructure.ai;

public record AiNextQuestionSlotStatus(
        String slotName,
        String description,
        boolean filled,
        AiSlotEvidencePolicy evidencePolicy
) {
}

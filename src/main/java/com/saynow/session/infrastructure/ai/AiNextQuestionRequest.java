// 꼬리 질문 생성을 위해 AI 서버에 전달하는 요청 DTO
package com.saynow.session.infrastructure.ai;

import java.util.List;

public record AiNextQuestionRequest(
        String originalQuestion,
        String userUtterance,
        String scenarioTitle,
        String aiRole,
        String scenarioSituation,
        String scenarioGoal,
        List<AiSlotStatus> slots
) {
}

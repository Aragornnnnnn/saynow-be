// 완료된 세션의 최종 피드백 생성을 요청하는 AI DTO
package com.saynow.session.infrastructure.ai;

import java.util.List;

public record AiFeedbackRequest(
        String scenarioTitle,
        String aiRole,
        String scenarioSituation,
        String scenarioGoal,
        String sessionResult,
        List<AiSlotStatus> slots,
        List<AiFeedbackTurnRequest> turns
) {
}

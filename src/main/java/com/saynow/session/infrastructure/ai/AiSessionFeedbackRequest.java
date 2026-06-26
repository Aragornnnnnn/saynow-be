// 세션 최종 피드백 생성을 AI 서버에 요청하는 DTO
package com.saynow.session.infrastructure.ai;

import java.util.List;

public record AiSessionFeedbackRequest(
        Long sessionId,
        AiScenarioContext scenario,
        List<Long> expectedTurnIds
) {
}

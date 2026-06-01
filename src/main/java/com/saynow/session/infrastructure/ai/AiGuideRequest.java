// 영어 학습 가이드 답변 생성을 위해 AI 서버에 전달하는 요청 DTO
package com.saynow.session.infrastructure.ai;

public record AiGuideRequest(
        String question,
        String scenarioTitle,
        String scenarioGoal,
        String scenarioSituation,
        String aiRole
) {
}

// AI 서버가 생성한 턴별 피드백 결과를 담는 값 객체
package com.saynow.practice.infrastructure.ai;

public record AiTurnFeedbackResult(
        int understoodScore,
        String heardAs,
        String betterExpression,
        int scoreDelta,
        int improvedUnderstoodScore,
        String reason
) {
}

// AI 서버가 생성한 세션 최종 피드백 결과를 담는 값 객체
package com.saynow.practice.infrastructure.ai;

import java.util.List;

public record AiSessionFeedbackResult(
        int totalUnderstoodScore,
        String summary,
        List<AiTurnFeedbackResult> turnFeedback
) {
}

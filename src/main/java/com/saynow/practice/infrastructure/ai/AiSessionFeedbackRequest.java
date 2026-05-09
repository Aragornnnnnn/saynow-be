// AI 서버에 세션 최종 피드백 생성을 요청할 때 사용하는 값 객체
package com.saynow.practice.infrastructure.ai;

import com.saynow.practice.domain.PracticeTurn;
import com.saynow.practice.domain.SessionStatus;
import com.saynow.scenario.domain.Scenario;

import java.util.List;

public record AiSessionFeedbackRequest(
        Scenario scenario,
        SessionStatus scenarioResult,
        List<PracticeTurn> turns
) {
}

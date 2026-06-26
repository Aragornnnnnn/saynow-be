// 발화 직후 AI 턴별 피드백 캐시 준비 상태를 표현한다.
package com.saynow.session.infrastructure.ai;

public enum TurnFeedbackStatus {
    PREPARING,
    READY,
    FAILED
}

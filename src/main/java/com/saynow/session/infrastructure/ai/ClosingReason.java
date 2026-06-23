// AI 종료 메시지 생성 요청에 전달하는 대화 종료 사유
package com.saynow.session.infrastructure.ai;

public enum ClosingReason {
    GOAL_COMPLETED,
    MAX_TURNS_REACHED,
    USER_ENDED,
    TIME_LIMIT_REACHED
}

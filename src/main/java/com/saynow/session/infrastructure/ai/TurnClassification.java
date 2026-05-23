// AI 서버가 분류한 사용자 발화의 유효성 상태를 표현한다.
package com.saynow.session.infrastructure.ai;

public enum TurnClassification {
    ANSWER,
    ASSISTANCE_REQUEST,
    INVALID_RESPONSE
}

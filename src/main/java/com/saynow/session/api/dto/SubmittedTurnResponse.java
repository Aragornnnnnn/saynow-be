// 제출된 사용자 발화 턴의 처리 상태를 반환하는 DTO
package com.saynow.session.api.dto;

import com.saynow.session.domain.InnerThoughtType;
import com.saynow.session.infrastructure.ai.TurnFeedbackStatus;

public record SubmittedTurnResponse(
        Long turnId,
        int sequence,
        TurnFeedbackStatus turnFeedbackStatus,
        String innerThought,
        InnerThoughtType innerThoughtType
) {
}

package com.saynow.practice.service.ai;

import com.saynow.practice.domain.SessionStatus;

import java.util.List;

public record AiTurnEvaluationResult(
        SessionStatus scenarioStatus,
        List<AiFilledSlot> filledSlots,
        AiPrompt nextPrompt,
        AiPrompt resultMessage
) {
}

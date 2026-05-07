package com.saynow.practice.infrastructure.ai;

import com.saynow.practice.domain.SessionStatus;

import java.math.BigDecimal;
import java.util.List;

public record AiTurnEvaluationResult(
        String transcript,
        BigDecimal sttConfidence,
        SessionStatus scenarioStatus,
        List<AiFilledSlot> filledSlots,
        AiPrompt nextPrompt,
        AiPrompt resultMessage
) {
}

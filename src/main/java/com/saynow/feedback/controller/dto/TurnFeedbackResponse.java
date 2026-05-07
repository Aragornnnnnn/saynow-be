package com.saynow.feedback.controller.dto;

public record TurnFeedbackResponse(
        Long turnId,
        int turnIndex,
        String questionText,
        String userTranscript,
        Integer speechStartedAfterMs,
        Double speechStartedAfterSeconds,
        int understoodScore,
        String heardAs,
        String betterExpression,
        int scoreDelta,
        int improvedUnderstoodScore,
        String reason
) {
}

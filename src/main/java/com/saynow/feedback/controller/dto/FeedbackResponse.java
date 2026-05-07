package com.saynow.feedback.controller.dto;

import com.saynow.practice.domain.SessionStatus;

import java.util.List;

public record FeedbackResponse(
        String sessionId,
        SessionStatus scenarioResult,
        int totalUnderstoodScore,
        String summary,
        int averageScoreDelta,
        List<TurnFeedbackResponse> turnFeedback
) {
}

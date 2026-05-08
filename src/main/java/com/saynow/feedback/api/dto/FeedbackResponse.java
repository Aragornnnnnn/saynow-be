package com.saynow.feedback.api.dto;

import com.saynow.practice.domain.SessionStatus;

import java.util.List;

public record FeedbackResponse(
        String sessionId,
        SessionStatus scenarioResult,
        int totalUnderstoodScore,
        String summary,
        List<TurnFeedbackResponse> turnFeedback
) {
}

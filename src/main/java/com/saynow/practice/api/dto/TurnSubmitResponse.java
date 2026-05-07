package com.saynow.practice.api.dto;

import com.saynow.practice.domain.SessionStatus;

public record TurnSubmitResponse(
        String sessionId,
        Long turnId,
        int turnIndex,
        String transcript,
        SessionStatus status,
        String babsaeText,
        String babsaeTtsUrl,
        long followUpCount,
        int maxFollowUpCount,
        boolean feedbackAvailable
) {
}

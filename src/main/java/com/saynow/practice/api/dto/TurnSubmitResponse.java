package com.saynow.practice.api.dto;

import com.saynow.practice.domain.SessionStatus;

import java.math.BigDecimal;

public record TurnSubmitResponse(
        String sessionId,
        Long turnId,
        int turnIndex,
        String transcript,
        BigDecimal sttConfidence,
        SessionStatus status,
        String babsaeText,
        String babsaeTtsUrl,
        long followUpCount,
        int maxFollowUpCount,
        boolean feedbackAvailable
) {
}

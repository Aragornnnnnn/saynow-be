package com.saynow.practice.controller.dto;

import com.saynow.practice.domain.SessionStatus;

import java.util.List;

public record SessionStatusResponse(
        String sessionId,
        String scenarioId,
        SessionStatus status,
        String babsaeText,
        String babsaeTtsUrl,
        long followUpCount,
        int maxFollowUpCount,
        Long micReadyLatencyMs,
        List<TurnHistoryResponse> turns
) {
}

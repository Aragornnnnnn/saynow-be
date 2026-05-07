package com.saynow.practice.controller.dto;

import com.saynow.practice.domain.SessionStatus;

import java.time.LocalDateTime;

public record SessionStartResponse(
        String sessionId,
        String scenarioId,
        SessionStatus status,
        String babsaeText,
        String babsaeTtsUrl,
        long followUpCount,
        int maxFollowUpCount,
        LocalDateTime startedAt
) {
}

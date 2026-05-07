package com.saynow.practice.controller.dto;

import java.time.LocalDateTime;

public record TurnHistoryResponse(
        Long turnId,
        int turnIndex,
        String questionText,
        String userTranscript,
        Integer speechStartedAfterMs,
        Integer recordingDurationMs,
        LocalDateTime createdAt
) {
}

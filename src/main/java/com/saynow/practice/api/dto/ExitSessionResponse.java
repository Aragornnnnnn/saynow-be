package com.saynow.practice.api.dto;

import com.saynow.practice.domain.SessionStatus;

import java.time.LocalDateTime;

public record ExitSessionResponse(
        String sessionId,
        SessionStatus status,
        LocalDateTime endedAt
) {
}

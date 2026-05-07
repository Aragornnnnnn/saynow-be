package com.saynow.practice.api.dto;

public record MicReadyResponse(
        String sessionId,
        long micReadyLatencyMs
) {
}

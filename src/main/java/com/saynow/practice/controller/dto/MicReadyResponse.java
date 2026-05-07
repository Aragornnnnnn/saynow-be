package com.saynow.practice.controller.dto;

public record MicReadyResponse(
        String sessionId,
        long micReadyLatencyMs
) {
}

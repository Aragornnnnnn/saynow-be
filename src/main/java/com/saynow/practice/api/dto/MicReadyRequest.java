package com.saynow.practice.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MicReadyRequest(
        @NotNull @PositiveOrZero Integer latencyMs
) {
}

package com.saynow.practice.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MicReadyRequest(
        @NotNull @PositiveOrZero Long latencyMs
) {
}

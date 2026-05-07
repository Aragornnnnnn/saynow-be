package com.saynow.practice.api.dto;

import com.saynow.practice.domain.InputType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SubmitTurnRequest(
        @NotNull InputType inputType,
        @PositiveOrZero Integer speechStartedAfterMs,
        @PositiveOrZero Integer recordingDurationMs
) {
}

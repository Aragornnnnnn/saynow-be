package com.saynow.practice.api.dto;

import com.saynow.practice.domain.InputType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record SubmitTurnRequest(
        @NotBlank String transcript,
        @NotNull InputType inputType,
        @PositiveOrZero Integer speechStartedAfterMs,
        @PositiveOrZero Integer recordingDurationMs,
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal sttConfidence
) {
}

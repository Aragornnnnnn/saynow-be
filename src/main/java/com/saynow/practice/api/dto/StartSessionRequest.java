package com.saynow.practice.api.dto;

import jakarta.validation.constraints.NotBlank;

public record StartSessionRequest(
        @NotBlank String scenarioId
) {
}

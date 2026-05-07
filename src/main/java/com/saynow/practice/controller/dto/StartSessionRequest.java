package com.saynow.practice.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record StartSessionRequest(
        @NotBlank String scenarioId
) {
}

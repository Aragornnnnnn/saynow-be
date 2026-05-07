package com.saynow.practice.controller.dto;

import jakarta.validation.constraints.Size;

public record ExitSessionRequest(
        @Size(max = 100) String reason
) {
}

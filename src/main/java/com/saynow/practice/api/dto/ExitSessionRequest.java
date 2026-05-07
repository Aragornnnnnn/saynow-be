package com.saynow.practice.api.dto;

import jakarta.validation.constraints.Size;

public record ExitSessionRequest(
        @Size(max = 100) String reason
) {
}

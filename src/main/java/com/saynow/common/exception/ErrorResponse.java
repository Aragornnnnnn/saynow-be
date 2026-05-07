package com.saynow.common.exception;

public record ErrorResponse(
        String code,
        String message
) {
}

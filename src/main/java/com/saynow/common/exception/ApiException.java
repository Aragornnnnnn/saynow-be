package com.saynow.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    @Getter
    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return errorCode.getStatus();
    }

}

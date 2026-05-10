package com.saynow.common.exception;

import com.saynow.common.observability.SentryEventReporter;
import com.saynow.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final SentryEventReporter sentryEventReporter;

    public GlobalExceptionHandler(SentryEventReporter sentryEventReporter) {
        this.sentryEventReporter = sentryEventReporter;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
        sentryEventReporter.captureException(exception);
        return ResponseEntity.status(exception.getStatus())
                .body(ApiResponse.error(exception.getErrorCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        return error(ErrorCode.VALIDATION_FAILED, exception);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        return error(ErrorCode.VALIDATION_FAILED, exception);
    }

    @ExceptionHandler({
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
        return error(ErrorCode.VALIDATION_FAILED, exception);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return error(ErrorCode.UNSUPPORTED_AUDIO_TYPE, exception);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        return error(ErrorCode.AUDIO_TOO_LARGE, exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        return error(ErrorCode.INTERNAL_SERVER_ERROR, exception);
    }

    private ResponseEntity<ApiResponse<Void>> error(ErrorCode errorCode, Exception exception) {
        sentryEventReporter.captureException(exception);
        return error(errorCode);
    }

    private ResponseEntity<ApiResponse<Void>> error(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode));
    }
}

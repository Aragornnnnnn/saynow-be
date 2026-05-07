package com.saynow.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    SCENARIO_NOT_FOUND(HttpStatus.NOT_FOUND, "시나리오를 찾을 수 없습니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."),
    PROMPT_NOT_FOUND(HttpStatus.NOT_FOUND, "세션 질문을 찾을 수 없습니다."),
    FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "피드백을 찾을 수 없습니다."),
    FEEDBACK_FAILED(HttpStatus.NOT_FOUND, "피드백 생성에 실패했습니다."),

    SESSION_ALREADY_ENDED(HttpStatus.CONFLICT, "이미 종료된 세션입니다."),
    SESSION_IN_PROGRESS(HttpStatus.BAD_REQUEST, "아직 진행 중인 세션입니다."),

    AUDIO_REQUIRED(HttpStatus.BAD_REQUEST, "음성 파일이 필요합니다."),
    AUDIO_READ_FAILED(HttpStatus.BAD_REQUEST, "음성 파일을 읽을 수 없습니다."),
    AUDIO_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "음성 파일 크기 제한을 초과했습니다."),
    UNSUPPORTED_AUDIO_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 음성 파일 형식입니다."),
    UNSUPPORTED_INPUT_TYPE(HttpStatus.BAD_REQUEST, "MVP에서는 AUDIO 입력만 지원합니다."),

    AI_STT_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버가 transcript를 반환하지 않았습니다."),
    AI_RESPONSE_INVALID(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버 응답이 올바르지 않습니다."),
    FEEDBACK_GENERATING(HttpStatus.ACCEPTED, "피드백을 생성 중입니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}

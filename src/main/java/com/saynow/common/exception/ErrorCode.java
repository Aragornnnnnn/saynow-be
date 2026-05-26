package com.saynow.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료됐습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "재발급 토큰이 올바르지 않습니다."),
    UNSUPPORTED_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 제공자입니다."),
    OIDC_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "소셜 로그인 토큰이 올바르지 않습니다."),
    OIDC_NONCE_MISMATCH(HttpStatus.BAD_REQUEST, "소셜 로그인 요청 검증 값이 일치하지 않습니다."),
    SOCIAL_ACCOUNT_CONFLICT(HttpStatus.CONFLICT, "이미 다른 계정에 연결된 소셜 계정입니다."),
    OIDC_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "소셜 로그인 제공자 검증에 실패했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "세션에 접근할 권한이 없습니다."),

    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    SCENARIO_NOT_FOUND(HttpStatus.NOT_FOUND, "시나리오를 찾을 수 없습니다."),
    CATEGORY_LOCKED(HttpStatus.FORBIDDEN, "잠긴 카테고리입니다."),
    SCENARIO_LOCKED(HttpStatus.FORBIDDEN, "잠긴 시나리오입니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 세션입니다."),
    FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "피드백을 찾을 수 없습니다."),

    SESSION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료된 세션입니다."),
    SESSION_IN_PROGRESS(HttpStatus.BAD_REQUEST, "아직 진행 중인 세션입니다."),
    SESSION_NOT_COMPLETABLE(HttpStatus.CONFLICT, "세션을 완료할 수 없습니다."),

    AUDIO_REQUIRED(HttpStatus.BAD_REQUEST, "음성 파일이 필요합니다."),
    AUDIO_READ_FAILED(HttpStatus.BAD_REQUEST, "음성 파일을 읽을 수 없습니다."),
    AUDIO_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "음성 파일 크기 제한을 초과했습니다."),
    UNSUPPORTED_AUDIO_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 음성 파일 형식입니다."),
    UNSUPPORTED_INPUT_TYPE(HttpStatus.BAD_REQUEST, "MVP에서는 AUDIO 입력만 지원합니다."),

    AI_STT_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버가 transcript를 반환하지 않았습니다."),
    AI_RESPONSE_INVALID(HttpStatus.SERVICE_UNAVAILABLE, "AI 응답을 처리할 수 없습니다."),
    AI_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "가이드 답변 생성에 실패했습니다."),
    FEEDBACK_GENERATION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "피드백 생성에 실패했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}

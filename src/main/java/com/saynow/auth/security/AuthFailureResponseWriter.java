// 인증 실패를 공통 API 오류 응답으로 쓰는 컴포넌트
package com.saynow.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.common.observability.SentryEventReporter;
import com.saynow.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AuthFailureResponseWriter {

    private final ObjectMapper objectMapper;
    private final SentryEventReporter sentryEventReporter;

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        sentryEventReporter.captureException(new ApiException(errorCode));
        response.setStatus(errorCode.getStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
    }
}

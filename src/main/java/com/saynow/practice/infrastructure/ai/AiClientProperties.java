// AI 서버 연동 설정 값을 바인딩하는 프로퍼티
package com.saynow.practice.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "saynow.ai")
public record AiClientProperties(
        URI baseUrl,
        String clientMode,
        String turnEvaluationPath,
        String sessionFeedbackPath
) {

    public AiClientProperties {
        if (turnEvaluationPath == null || turnEvaluationPath.isBlank()) {
            turnEvaluationPath = "/api/v1/turn-evaluations";
        }
        if (sessionFeedbackPath == null || sessionFeedbackPath.isBlank()) {
            sessionFeedbackPath = "/api/v1/session-feedbacks";
        }
    }
}

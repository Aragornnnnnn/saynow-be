// AI 서버 연동 설정 값을 바인딩하는 프로퍼티
package com.saynow.session.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.net.URI;

@ConfigurationProperties(prefix = "saynow.ai")
public record AiClientProperties(
        URI baseUrl,
        String clientMode,
        String nextQuestionPath,
        String closingMessagePath,
        String turnFeedbackPath,
        String sessionFeedbackPath,
        Duration requestTimeout
) {

    public AiClientProperties {
        if (nextQuestionPath == null || nextQuestionPath.isBlank()) {
            nextQuestionPath = "/api/v1/conversation/next-question";
        }
        if (closingMessagePath == null || closingMessagePath.isBlank()) {
            closingMessagePath = "/api/v1/conversation/closing-message";
        }
        if (turnFeedbackPath == null || turnFeedbackPath.isBlank()) {
            turnFeedbackPath = "/api/v1/conversation/turn-feedback";
        }
        if (sessionFeedbackPath == null || sessionFeedbackPath.isBlank()) {
            sessionFeedbackPath = "/api/v1/conversation/session-feedback";
        }
        if (requestTimeout == null || requestTimeout.isNegative() || requestTimeout.isZero()) {
            requestTimeout = Duration.ofSeconds(180);
        }
    }
}

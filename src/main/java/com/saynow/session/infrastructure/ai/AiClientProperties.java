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
        String feedbackPath,
        String feedbackStreamPath,
        String guidePath,
        Duration feedbackStreamTimeout
) {

    public AiClientProperties {
        if (nextQuestionPath == null || nextQuestionPath.isBlank()) {
            nextQuestionPath = "/api/v1/conversation/next-question";
        }
        if (feedbackPath == null || feedbackPath.isBlank()) {
            feedbackPath = "/api/v1/conversation/feedback";
        }
        if (feedbackStreamPath == null || feedbackStreamPath.isBlank()) {
            feedbackStreamPath = "/api/v1/conversation/feedback/stream";
        }
        if (guidePath == null || guidePath.isBlank()) {
            guidePath = "/api/v1/conversation/guide";
        }
        if (feedbackStreamTimeout == null || feedbackStreamTimeout.isNegative() || feedbackStreamTimeout.isZero()) {
            feedbackStreamTimeout = Duration.ofSeconds(180);
        }
    }
}

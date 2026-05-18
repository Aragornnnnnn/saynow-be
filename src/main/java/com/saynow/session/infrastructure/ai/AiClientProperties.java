// AI 서버 연동 설정 값을 바인딩하는 프로퍼티
package com.saynow.session.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "saynow.ai")
public record AiClientProperties(
        URI baseUrl,
        String clientMode,
        String nextQuestionPath,
        String feedbackPath
) {

    public AiClientProperties {
        if (nextQuestionPath == null || nextQuestionPath.isBlank()) {
            nextQuestionPath = "/api/v1/conversation/next-question";
        }
        if (feedbackPath == null || feedbackPath.isBlank()) {
            feedbackPath = "/api/v1/conversation/feedback";
        }
    }
}

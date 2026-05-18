// 원격 AI 서버의 꼬리 질문과 최종 피드백 API를 호출하는 클라이언트
package com.saynow.session.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "saynow.ai", name = "client-mode", havingValue = "remote")
@RequiredArgsConstructor
@Slf4j
public class RemoteAiConversationClient implements AiConversationClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final AiClientProperties properties;

    @Override
    public AiNextQuestionResponse generateNextQuestion(AiNextQuestionRequest request) {
        return post(nextQuestionUri(), request, RemoteNextQuestionResponse.class).toResponse();
    }

    @Override
    public AiFeedbackResponse generateFeedback(AiFeedbackRequest request) {
        return post(feedbackUri(), request, RemoteFeedbackResponse.class).toResponse();
    }

    private <T> T post(URI uri, Object payload, Class<T> responseType) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                log.warn("AI 서버 호출이 실패했습니다. uri={}, status={}, body={}", uri, httpResponse.statusCode(), httpResponse.body());
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 호출에 실패했습니다.");
            }
            return objectMapper.readValue(httpResponse.body(), responseType);
        } catch (ApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 호출에 실패했습니다.");
        } catch (IOException exception) {
            log.warn("AI 서버 호출에 실패했습니다.", exception);
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 호출에 실패했습니다.");
        }
    }

    private URI nextQuestionUri() {
        return properties.baseUrl().resolve(properties.nextQuestionPath());
    }

    private URI feedbackUri() {
        return properties.baseUrl().resolve(properties.feedbackPath());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteNextQuestionResponse(
            String nextQuestion,
            String translatedQuestion,
            List<RemoteFilledSlot> filledSlots
    ) {

        private AiNextQuestionResponse toResponse() {
            if (filledSlots == null) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 꼬리 질문 응답이 올바르지 않습니다.");
            }
            return new AiNextQuestionResponse(
                    nextQuestion,
                    translatedQuestion,
                    filledSlots.stream()
                            .map(slot -> new AiFilledSlot(slot.slotName()))
                            .toList());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteFilledSlot(String slotName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteFeedbackResponse(
            Integer comprehensionScore,
            String feedbackSummary,
            List<RemoteTurnFeedback> turnFeedbacks
    ) {

        private AiFeedbackResponse toResponse() {
            if (comprehensionScore == null || feedbackSummary == null || turnFeedbacks == null) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 피드백 응답이 올바르지 않습니다.");
            }
            return new AiFeedbackResponse(
                    comprehensionScore,
                    feedbackSummary,
                    turnFeedbacks.stream()
                            .map(RemoteTurnFeedback::toResponse)
                            .toList());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteTurnFeedback(
            Long turnId,
            Boolean feedbackRequired,
            String nativeUnderstanding,
            String nativeLanguageInterpretation,
            String betterExpression
    ) {

        private AiTurnFeedbackResponse toResponse() {
            if (turnId == null || feedbackRequired == null) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 턴 피드백 응답이 올바르지 않습니다.");
            }
            return new AiTurnFeedbackResponse(
                    turnId,
                    feedbackRequired,
                    nativeUnderstanding,
                    nativeLanguageInterpretation,
                    betterExpression);
        }
    }
}

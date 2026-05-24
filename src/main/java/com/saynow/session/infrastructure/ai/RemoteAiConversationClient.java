// 원격 AI 서버의 꼬리 질문과 최종 피드백 API를 호출하는 클라이언트
package com.saynow.session.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

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
public class RemoteAiConversationClient implements AiConversationClient, AiFeedbackStreamClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final AiClientProperties properties;
    private final WebClient.Builder webClientBuilder;

    @Override
    public AiNextQuestionResponse generateNextQuestion(AiNextQuestionRequest request) {
        return post(nextQuestionUri(), request, RemoteNextQuestionResponse.class).toResponse();
    }

    @Override
    public AiFeedbackResponse generateFeedback(AiFeedbackRequest request) {
        return post(feedbackUri(), request, RemoteFeedbackResponse.class).toResponse();
    }

    @Override
    public Flux<AiFeedbackStreamEvent> streamFeedback(AiFeedbackRequest request) {
        URI uri = feedbackStreamUri();
        return webClientBuilder.build()
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> {
                            log.warn("AI 서버 SSE 호출이 실패했습니다. uri={}, status={}, body={}", uri, response.statusCode(), body);
                            return new AiFeedbackStreamException("AI 서버 SSE 호출에 실패했습니다.");
                        }))
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                })
                .timeout(properties.feedbackStreamTimeout())
                .take(properties.feedbackStreamTimeout())
                .filter(event -> event.event() != null && event.data() != null)
                .map(event -> new AiFeedbackStreamEvent(event.event(), event.data()))
                .onErrorMap(exception -> !(exception instanceof AiFeedbackStreamException),
                        exception -> {
                            log.warn("AI 서버 SSE 연결에 실패했습니다. uri={}", uri, exception);
                            return new AiFeedbackStreamException("AI 서버 SSE 연결에 실패했습니다.", exception);
                        });
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

    private URI feedbackStreamUri() {
        return properties.baseUrl().resolve(properties.feedbackStreamPath());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteNextQuestionResponse(
            String nextQuestion,
            String translatedQuestion,
            List<RemoteFilledSlot> filledSlots,
            TurnClassification turnClassification
    ) {

        private AiNextQuestionResponse toResponse() {
            if (filledSlots == null || turnClassification == null) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 꼬리 질문 응답이 올바르지 않습니다.");
            }
            return new AiNextQuestionResponse(
                    nextQuestion,
                    translatedQuestion,
                    filledSlots.stream()
                            .map(RemoteFilledSlot::toResponse)
                            .toList(),
                    turnClassification);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteFilledSlot(String slotName) {

        private AiFilledSlot toResponse() {
            if (slotName == null || slotName.isBlank()) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 꼬리 질문 응답이 올바르지 않습니다.");
            }
            return new AiFilledSlot(slotName);
        }
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

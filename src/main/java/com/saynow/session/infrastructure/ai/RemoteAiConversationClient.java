// 원격 AI 서버의 꼬리 질문과 최종 피드백 API를 호출하는 클라이언트
package com.saynow.session.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.common.observability.RequestTraceContext;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

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
        return post("next_question", nextQuestionUri(), request, RemoteNextQuestionResponse.class).toResponse();
    }

    @Override
    public AiFeedbackResponse generateFeedback(AiFeedbackRequest request) {
        return post("feedback", feedbackUri(), request, RemoteFeedbackResponse.class).toResponse();
    }

    @Override
    public AiGuideResponse generateGuide(AiGuideRequest request) {
        return post(
                "guide",
                guideUri(),
                request,
                RemoteGuideResponse.class,
                ErrorCode.AI_GENERATION_FAILED,
                ErrorCode.AI_GENERATION_FAILED.getMessage()).toResponse();
    }

    @Override
    public Flux<AiFeedbackStreamEvent> streamFeedback(AiFeedbackRequest request) {
        URI uri = feedbackStreamUri();
        Optional<String> requestId = RequestTraceContext.currentRequestId();
        String requestIdForLog = requestId.orElse("none");
        AtomicLong startedAt = new AtomicLong();
        return webClientBuilder.build()
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .headers(headers -> requestId.ifPresent(value -> headers.set(RequestTraceContext.REQUEST_ID_HEADER, value)))
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> {
                            log.warn(
                                    "AI 서버 SSE 호출이 실패했습니다. requestId={} uri={} status={} body={}",
                                    requestIdForLog,
                                    uri,
                                    response.statusCode(),
                                    body);
                            return new AiFeedbackStreamException("AI 서버 SSE 호출에 실패했습니다.");
                        }))
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                })
                .doOnSubscribe(subscription -> startedAt.set(System.nanoTime()))
                .timeout(properties.feedbackStreamTimeout())
                .take(properties.feedbackStreamTimeout())
                .filter(event -> event.event() != null && event.data() != null)
                .map(event -> new AiFeedbackStreamEvent(event.event(), event.data()))
                .onErrorMap(exception -> !(exception instanceof AiFeedbackStreamException),
                        exception -> {
                            log.warn("AI 서버 SSE 연결에 실패했습니다. requestId={} uri={}", requestIdForLog, uri, exception);
                            return new AiFeedbackStreamException("AI 서버 SSE 연결에 실패했습니다.", exception);
                        })
                .doFinally(signalType -> {
                    if (startedAt.get() > 0) {
                        logAiCallLatency("feedback_stream", uri, requestIdForLog, signalType.name(), System.nanoTime() - startedAt.get());
                    }
                });
    }

    private <T> T post(String workflow, URI uri, Object payload, Class<T> responseType) {
        return post(workflow, uri, payload, responseType, ErrorCode.AI_RESPONSE_INVALID, "AI 서버 호출에 실패했습니다.");
    }

    private <T> T post(String workflow, URI uri, Object payload, Class<T> responseType, ErrorCode errorCode, String message) {
        Optional<String> requestId = RequestTraceContext.currentRequestId();
        String requestIdForLog = requestId.orElse("none");
        long startedAt = System.nanoTime();
        String status = "none";
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8));
            requestId.ifPresent(value -> requestBuilder.header(RequestTraceContext.REQUEST_ID_HEADER, value));

            HttpRequest httpRequest = requestBuilder.build();
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            status = String.valueOf(httpResponse.statusCode());
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                log.warn(
                        "AI 서버 호출이 실패했습니다. requestId={} uri={} status={} body={}",
                        requestIdForLog,
                        uri,
                        httpResponse.statusCode(),
                        httpResponse.body());
                throw new ApiException(errorCode, message);
            }
            return objectMapper.readValue(httpResponse.body(), responseType);
        } catch (ApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(errorCode, message);
        } catch (IOException exception) {
            log.warn("AI 서버 호출에 실패했습니다. requestId={} uri={}", requestIdForLog, uri, exception);
            throw new ApiException(errorCode, message);
        } finally {
            logAiCallLatency(workflow, uri, requestIdForLog, status, System.nanoTime() - startedAt);
        }
    }

    private void logAiCallLatency(String workflow, URI uri, String requestId, String status, long elapsedNanos) {
        long elapsedMs = RequestTraceContext.recordAiCall(elapsedNanos);
        log.info(
                "event=ai_call_latency requestId={} workflow={} uri={} status={} elapsedMs={}",
                requestId,
                workflow,
                uri.getPath(),
                status,
                elapsedMs);
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

    private URI guideUri() {
        return properties.baseUrl().resolve(properties.guidePath());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteNextQuestionResponse(
            String nextQuestion,
            String translatedQuestion,
            String nextQuestionTargetSlotName,
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
                    nextQuestionTargetSlotName,
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
    private record RemoteGuideResponse(String answer) {

        private AiGuideResponse toResponse() {
            if (answer == null || answer.isBlank()) {
                throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
            }
            return new AiGuideResponse(answer);
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

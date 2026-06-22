// 원격 AI 서버의 3차 MVP 대화와 피드백 API를 호출하는 클라이언트
package com.saynow.session.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.common.observability.RequestTraceContext;
import com.saynow.session.domain.InnerThoughtType;
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
import java.util.Optional;

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
        return post(
                "next_question",
                nextQuestionUri(),
                request,
                RemoteNextQuestionResponse.class,
                ErrorCode.AI_GENERATION_FAILED,
                ErrorCode.AI_GENERATION_FAILED.getMessage()).toResponse();
    }

    @Override
    public AiTurnFeedbackStatusResponse generateTurnFeedback(AiTurnFeedbackRequest request) {
        return post("turn_feedback", turnFeedbackUri(), request, RemoteTurnFeedbackStatusResponse.class).toResponse();
    }

    @Override
    public AiSessionFeedbackResponse generateSessionFeedback(AiSessionFeedbackRequest request) {
        return post(
                "session_feedback",
                sessionFeedbackUri(),
                request,
                RemoteSessionFeedbackResponse.class,
                ErrorCode.FEEDBACK_GENERATION_FAILED,
                ErrorCode.FEEDBACK_GENERATION_FAILED.getMessage()).toResponse();
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

            HttpResponse<String> httpResponse = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            status = String.valueOf(httpResponse.statusCode());
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                log.warn(
                        "AI 서버 호출이 실패했습니다. requestId={} uri={} status={} body={}",
                        requestIdForLog,
                        uri,
                        httpResponse.statusCode(),
                        httpResponse.body());
                if (errorCode == ErrorCode.FEEDBACK_GENERATION_FAILED && httpResponse.statusCode() == 409) {
                    throw new ApiException(ErrorCode.FEEDBACK_NOT_READY);
                }
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

    private URI turnFeedbackUri() {
        return properties.baseUrl().resolve(properties.turnFeedbackPath());
    }

    private URI sessionFeedbackUri() {
        return properties.baseUrl().resolve(properties.sessionFeedbackPath());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteNextQuestionResponse(
            String aiQuestion,
            String translatedQuestion,
            String innerThought,
            InnerThoughtType innerThoughtType
    ) {

        private AiNextQuestionResponse toResponse() {
            if (aiQuestion == null || aiQuestion.isBlank()
                    || translatedQuestion == null || translatedQuestion.isBlank()
                    || innerThought == null || innerThought.isBlank()
                    || innerThoughtType == null) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 다음 질문 응답이 올바르지 않습니다.");
            }
            return new AiNextQuestionResponse(aiQuestion, translatedQuestion, innerThought, innerThoughtType);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteTurnFeedbackStatusResponse(
            Long sessionId,
            Long turnId,
            TurnFeedbackStatus feedbackStatus
    ) {

        private AiTurnFeedbackStatusResponse toResponse() {
            if (sessionId == null || turnId == null || feedbackStatus == null) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 턴별 피드백 응답이 올바르지 않습니다.");
            }
            return new AiTurnFeedbackStatusResponse(sessionId, turnId, feedbackStatus);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteSessionFeedbackResponse(
            Long sessionId,
            Integer nativeScore,
            String highlightMessage,
            List<RemoteSessionTurnFeedback> turnFeedbacks
    ) {

        private AiSessionFeedbackResponse toResponse() {
            if (sessionId == null
                    || nativeScore == null
                    || nativeScore < 0
                    || nativeScore > 100
                    || highlightMessage == null
                    || highlightMessage.isBlank()
                    || turnFeedbacks == null) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 최종 피드백 응답이 올바르지 않습니다.");
            }
            return new AiSessionFeedbackResponse(
                    sessionId,
                    nativeScore,
                    highlightMessage,
                    turnFeedbacks.stream()
                            .map(RemoteSessionTurnFeedback::toResponse)
                            .toList());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteSessionTurnFeedback(
            Long turnId,
            FeedbackType feedbackType,
            String koreanAnalogy,
            String positiveFeedback,
            String feedbackDetail,
            String correctionExpression,
            String correctionReason,
            String benchmarkMessage
    ) {

        private AiSessionTurnFeedbackResponse toResponse() {
            if (turnId == null
                    || feedbackType == null
                    || koreanAnalogy == null
                    || koreanAnalogy.isBlank()
                    || !isValidFeedbackPayload()) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 턴별 피드백 응답이 올바르지 않습니다.");
            }
            return new AiSessionTurnFeedbackResponse(
                    turnId,
                    feedbackType,
                    koreanAnalogy,
                    positiveFeedback,
                    feedbackDetail,
                    correctionExpression,
                    correctionReason,
                    benchmarkMessage);
        }

        private boolean isValidFeedbackPayload() {
            if (feedbackType == FeedbackType.GOOD) {
                return isBlank(positiveFeedback)
                        && !isBlank(feedbackDetail)
                        && isBlank(correctionExpression)
                        && isBlank(correctionReason);
            }
            return !isBlank(positiveFeedback)
                    && isBlank(feedbackDetail)
                    && !isBlank(correctionExpression)
                    && !isBlank(correctionReason)
                    && isBlank(benchmarkMessage);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

// 원격 AI 서버의 턴 평가 API를 호출하는 클라이언트
package com.saynow.practice.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.practice.domain.InputType;
import com.saynow.practice.domain.SessionStatus;
import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.ScenarioSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "saynow.ai", name = "client-mode", havingValue = "remote")
public class RemoteAiPracticeClient implements AiPracticeClient {

    private static final Logger log = LoggerFactory.getLogger(RemoteAiPracticeClient.class);
    private static final String CRLF = "\r\n";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final AiClientProperties properties;

    public RemoteAiPracticeClient(
            ObjectMapper objectMapper,
            AiClientProperties properties
    ) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public AiTurnEvaluationResult evaluateTurn(AiTurnEvaluationRequest request) {
        try {
            String boundary = "saynow-" + UUID.randomUUID();
            HttpRequest httpRequest = HttpRequest.newBuilder(turnEvaluationUri())
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Accept", "application/json")
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody(boundary, request)))
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                log.warn("AI 서버 턴 평가 호출이 실패했습니다. status={}, body={}", httpResponse.statusCode(), httpResponse.body());
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 턴 평가 호출에 실패했습니다.");
            }

            RemoteTurnEvaluationResponse response = objectMapper.readValue(httpResponse.body(), RemoteTurnEvaluationResponse.class);
            if (response == null) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버가 빈 응답을 반환했습니다.");
            }
            return response.toResult();
        } catch (ApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("AI 서버 턴 평가 호출에 실패했습니다.", exception);
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 턴 평가 호출에 실패했습니다.");
        } catch (IOException exception) {
            log.warn("AI 서버 턴 평가 호출에 실패했습니다.", exception);
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 턴 평가 호출에 실패했습니다.");
        }
    }

    @Override
    public AiSessionFeedbackResult createSessionFeedback(AiSessionFeedbackRequest request) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(sessionFeedbackUri())
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(sessionFeedbackPayload(request)),
                            StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                log.warn("AI 서버 세션 피드백 호출이 실패했습니다. status={}, body={}", httpResponse.statusCode(), httpResponse.body());
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 세션 피드백 호출에 실패했습니다.");
            }

            RemoteSessionFeedbackResponse response = objectMapper.readValue(httpResponse.body(), RemoteSessionFeedbackResponse.class);
            if (response == null) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버가 빈 응답을 반환했습니다.");
            }
            return response.toResult();
        } catch (ApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("AI 서버 세션 피드백 호출에 실패했습니다.", exception);
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 세션 피드백 호출에 실패했습니다.");
        } catch (IOException exception) {
            log.warn("AI 서버 세션 피드백 호출에 실패했습니다.", exception);
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 세션 피드백 호출에 실패했습니다.");
        }
    }

    private URI turnEvaluationUri() {
        return properties.baseUrl().resolve(properties.turnEvaluationPath());
    }

    private URI sessionFeedbackUri() {
        return properties.baseUrl().resolve(properties.sessionFeedbackPath());
    }

    private byte[] multipartBody(String boundary, AiTurnEvaluationRequest request) throws JsonProcessingException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        write(output, "--" + boundary + CRLF);
        write(output, "Content-Disposition: form-data; name=\"audio\"; filename=\"" + request.audioFilename() + "\"" + CRLF);
        write(output, "Content-Type: " + request.audioContentType() + CRLF);
        write(output, CRLF);
        output.writeBytes(request.audioContent());
        write(output, CRLF);

        write(output, "--" + boundary + CRLF);
        write(output, "Content-Disposition: form-data; name=\"payload\"" + CRLF);
        write(output, "Content-Type: text/plain; charset=UTF-8" + CRLF);
        write(output, CRLF);
        write(output, objectMapper.writeValueAsString(payload(request)));
        write(output, CRLF);
        write(output, "--" + boundary + "--" + CRLF);
        return output.toByteArray();
    }

    private void write(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private TurnEvaluationPayload payload(AiTurnEvaluationRequest request) {
        Scenario scenario = request.scenario();
        String categoryId = scenario.getCategory().getCategoryKey();
        List<ScenarioSlotPayload> scenarioSlots = request.scenarioSlots().stream()
                .map(slot -> new ScenarioSlotPayload(slot.getSlotKey(), slot.getDescription()))
                .toList();
        ScenarioPayload scenarioPayload = new ScenarioPayload(
                scenario.getScenarioKey(),
                categoryId,
                scenario.getTitle(),
                scenario.getSituationDescription(),
                scenario.getSuccessGoal(),
                scenario.getMaxFollowUpCount(),
                scenarioSlots);

        return new TurnEvaluationPayload(
                request.sessionId(),
                scenarioPayload,
                new CurrentQuestionPayload(request.currentQuestionText()),
                new TurnPayload(
                        request.turnIndex(),
                        request.inputType(),
                        request.speechStartedAfterMs(),
                        request.recordingDurationMs(),
                        request.currentFilledSlots()),
                request.currentFilledSlots(),
                request.followUpCount(),
                request.maxFollowUpCount());
    }

    private SessionFeedbackPayload sessionFeedbackPayload(AiSessionFeedbackRequest request) {
        return new SessionFeedbackPayload(
                request.sessionId(),
                new SessionFeedbackScenarioPayload(
                        request.scenario().getScenarioKey(),
                        request.scenario().getTitle(),
                        request.scenario().getSituationDescription(),
                        request.scenario().getSuccessGoal()),
                request.scenarioResult(),
                request.filledSlots().entrySet().stream()
                        .map(entry -> new SessionFeedbackFilledSlotPayload(entry.getKey(), entry.getValue()))
                        .toList(),
                request.turns().stream()
                        .map(turn -> new FeedbackTurnPayload(
                                turn.getId(),
                                turn.getTurnIndex(),
                                turn.getQuestionText(),
                                turn.getUserTranscript(),
                                turn.getSpeechStartedAfterMs(),
                                turn.getRecordingDurationMs()))
                        .toList());
    }

    private record TurnEvaluationPayload(
            String sessionId,
            ScenarioPayload scenario,
            CurrentQuestionPayload currentQuestion,
            TurnPayload turn,
            Map<String, String> currentFilledSlots,
            long followUpCount,
            int maxFollowUpCount
    ) {
    }

    private record ScenarioPayload(
            String scenarioId,
            String categoryId,
            String title,
            String situationDescription,
            String successGoal,
            int maxFollowUpCount,
            List<ScenarioSlotPayload> requiredSlots
    ) {
    }

    private record ScenarioSlotPayload(
            String slotKey,
            String description
    ) {
    }

    private record CurrentQuestionPayload(
            String questionText
    ) {
    }

    private record TurnPayload(
            int turnIndex,
            InputType inputType,
            Integer speechStartedAfterMs,
            Integer recordingDurationMs,
            Map<String, String> filledSlots
    ) {
    }

    private record SessionFeedbackPayload(
            String sessionId,
            SessionFeedbackScenarioPayload scenario,
            SessionStatus scenarioResult,
            List<SessionFeedbackFilledSlotPayload> filledSlots,
            List<FeedbackTurnPayload> turns
    ) {
    }

    private record SessionFeedbackScenarioPayload(
            String scenarioId,
            String title,
            String situationDescription,
            String successGoal
    ) {
    }

    private record SessionFeedbackFilledSlotPayload(
            String slotKey,
            String slotValue
    ) {
    }

    private record FeedbackTurnPayload(
            Long turnId,
            int turnIndex,
            String questionText,
            String userTranscript,
            Integer speechStartedAfterMs,
            Integer recordingDurationMs
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteTurnEvaluationResponse(
            String transcript,
            BigDecimal sttConfidence,
            SessionStatus scenarioStatus,
            List<RemoteFilledSlot> filledSlots,
            RemoteTtsContent nextQuestion,
            RemoteTtsContent resultMessage
    ) {

        private AiTurnEvaluationResult toResult() {
            List<AiFilledSlot> mappedFilledSlots = filledSlots == null
                    ? List.of()
                    : filledSlots.stream()
                    .map(slot -> new AiFilledSlot(slot.slotKey(), slot.slotValue()))
                    .toList();

            return new AiTurnEvaluationResult(
                    transcript,
                    sttConfidence,
                    scenarioStatus,
                    mappedFilledSlots,
                    toPrompt(nextQuestion),
                    toPrompt(resultMessage));
        }

        private AiPrompt toPrompt(RemoteTtsContent content) {
            if (content == null) {
                return null;
            }
            String text = firstNonBlank(content.questionText(), content.messageText());
            return text == null ? null : new AiPrompt(text, null);
        }

        private String firstNonBlank(String first, String second) {
            if (first != null && !first.isBlank()) {
                return first;
            }
            if (second != null && !second.isBlank()) {
                return second;
            }
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteFilledSlot(
            String slotKey,
            String slotValue
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteTtsContent(
            String questionText,
            String messageText,
            String ttsAudio
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteSessionFeedbackResponse(
            Integer totalUnderstoodScore,
            String summary,
            List<RemoteTurnFeedback> turns
    ) {

        private AiSessionFeedbackResult toResult() {
            if (totalUnderstoodScore == null || summary == null || turns == null) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 세션 피드백 응답이 올바르지 않습니다.");
            }
            return new AiSessionFeedbackResult(
                    totalUnderstoodScore,
                    summary,
                    turns.stream()
                            .map(RemoteTurnFeedback::toResult)
                            .toList());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteTurnFeedback(
            Integer understoodScore,
            String heardAs,
            String betterExpression,
            Integer scoreDelta,
            Integer improvedUnderstoodScore,
            String reason
    ) {

        private AiTurnFeedbackResult toResult() {
            if (understoodScore == null
                    || heardAs == null
                    || scoreDelta == null
                    || improvedUnderstoodScore == null
                    || reason == null) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 턴 피드백 응답이 올바르지 않습니다.");
            }
            return new AiTurnFeedbackResult(
                    understoodScore,
                    heardAs,
                    betterExpression,
                    scoreDelta,
                    improvedUnderstoodScore,
                    reason);
        }
    }
}

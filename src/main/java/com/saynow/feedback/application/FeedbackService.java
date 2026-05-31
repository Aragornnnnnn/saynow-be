// 완료 가능한 세션의 최종 피드백을 생성하고 조회 응답으로 조립하는 서비스
package com.saynow.feedback.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.common.observability.RequestTraceContext;
import com.saynow.feedback.api.dto.FeedbackResponse;
import com.saynow.feedback.api.dto.TurnFeedbackResponse;
import com.saynow.feedback.domain.SessionFeedback;
import com.saynow.feedback.domain.TurnFeedback;
import com.saynow.feedback.infrastructure.SessionFeedbackRepository;
import com.saynow.feedback.infrastructure.TurnFeedbackRepository;
import com.saynow.scenario.domain.ScenarioSlot;
import com.saynow.scenario.infrastructure.ScenarioSlotRepository;
import com.saynow.session.domain.Session;
import com.saynow.session.domain.SessionSlotStatus;
import com.saynow.session.domain.SessionTurn;
import com.saynow.session.domain.SessionStatus;
import com.saynow.session.infrastructure.SessionRepository;
import com.saynow.session.infrastructure.SessionSlotStatusRepository;
import com.saynow.session.infrastructure.SessionTurnRepository;
import com.saynow.session.infrastructure.ai.AiFeedbackStreamClient;
import com.saynow.session.infrastructure.ai.AiFeedbackStreamEvent;
import com.saynow.session.infrastructure.ai.AiFeedbackStreamException;
import com.saynow.session.infrastructure.ai.AiConversationClient;
import com.saynow.session.infrastructure.ai.AiFeedbackRequest;
import com.saynow.session.infrastructure.ai.AiFeedbackResponse;
import com.saynow.session.infrastructure.ai.AiFeedbackTurnRequest;
import com.saynow.session.infrastructure.ai.AiSlotStatus;
import com.saynow.session.infrastructure.ai.AiTurnFeedbackResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private static final int MAX_FEEDBACK_RETRY = 3;
    private static final String SSE_GENERATION_FAILED_CODE = "AI_GENERATION_FAILED";

    private final SessionRepository sessionRepository;
    private final SessionTurnRepository turnRepository;
    private final SessionSlotStatusRepository slotStatusRepository;
    private final ScenarioSlotRepository scenarioSlotRepository;
    private final SessionFeedbackRepository sessionFeedbackRepository;
    private final TurnFeedbackRepository turnFeedbackRepository;
    private final AiConversationClient aiConversationClient;
    private final AiFeedbackStreamClient aiFeedbackStreamClient;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public FeedbackResponse createFeedback(Long userId, Long sessionId) {
        long stageStartedAt = System.nanoTime();
        FeedbackContext context = loadFeedbackContext(userId, sessionId);
        logStageLatency("feedback", "load_context", userId, sessionId, stageStartedAt);

        stageStartedAt = System.nanoTime();
        AiFeedbackRequest request = toAiFeedbackRequest(context.session(), context.turns());
        logStageLatency("feedback", "prepare_ai_request", userId, sessionId, stageStartedAt);

        AiFeedbackResponse aiFeedback = generateFeedbackWithRetry(context.session(), request);

        stageStartedAt = System.nanoTime();
        validateAiFeedback(aiFeedback, context.turns());
        logStageLatency("feedback", "validate_ai_response", userId, sessionId, stageStartedAt);

        stageStartedAt = System.nanoTime();
        SessionFeedback sessionFeedback = sessionFeedbackRepository.save(new SessionFeedback(
                context.session(),
                context.session().getStatus() == SessionStatus.SUCCESS,
                aiFeedback.comprehensionScore(),
                aiFeedback.feedbackSummary()));
        logStageLatency("feedback", "save_session_feedback", userId, sessionId, stageStartedAt);

        stageStartedAt = System.nanoTime();
        Map<Long, AiTurnFeedbackResponse> aiFeedbackByTurnId = aiFeedback.turnFeedbacks().stream()
                .collect(Collectors.toMap(AiTurnFeedbackResponse::turnId, Function.identity()));
        List<TurnFeedback> savedTurnFeedbacks = context.turns().stream()
                .map(turn -> saveTurnFeedback(sessionFeedback, turn, aiFeedbackByTurnId.get(turn.getId())))
                .toList();
        logStageLatency("feedback", "save_turn_feedbacks", userId, sessionId, stageStartedAt);

        log.info(
                "세션 피드백 생성을 완료했습니다. userId={} sessionId={} cleared={} comprehensionScore={} turnCount={}",
                userId,
                sessionId,
                sessionFeedback.isCleared(),
                sessionFeedback.getComprehensionScore(),
                savedTurnFeedbacks.size());

        stageStartedAt = System.nanoTime();
        FeedbackResponse response = toResponse(context.session(), sessionFeedback, savedTurnFeedbacks);
        logStageLatency("feedback", "build_response", userId, sessionId, stageStartedAt);
        return response;
    }

    public StreamingResponseBody streamFeedback(Long userId, Long sessionId) {
        FeedbackStreamContext context = Objects.requireNonNull(transactionTemplate.execute(status -> {
            FeedbackContext feedbackContext = loadFeedbackContext(userId, sessionId);
            return new FeedbackStreamContext(
                    feedbackContext.session().getId(),
                    feedbackContext.session().getStatus() == SessionStatus.SUCCESS,
                    feedbackContext.turns().stream()
                            .map(SessionTurn::getId)
                            .toList(),
                    RequestTraceContext.currentRequestId().orElse(null),
                    toAiFeedbackRequest(feedbackContext.session(), feedbackContext.turns()));
        }));
        log.info(
                "세션 피드백 스트림 생성을 시작했습니다. userId={} sessionId={} cleared={} turnCount={}",
                userId,
                sessionId,
                context.cleared(),
                context.turnIds().size());
        return outputStream -> relayFeedbackStream(context, outputStream);
    }

    private FeedbackContext loadFeedbackContext(Long userId, Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
        if (!session.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        if (sessionFeedbackRepository.existsBySession(session)) {
            throw new ApiException(ErrorCode.SESSION_ALREADY_COMPLETED);
        }
        if (session.getStatus() != SessionStatus.SUCCESS && session.getStatus() != SessionStatus.FAILURE) {
            throw new ApiException(ErrorCode.SESSION_NOT_COMPLETABLE);
        }

        List<SessionTurn> turns = turnRepository.findBySessionOrderBySequenceAsc(session).stream()
                .filter(SessionTurn::isAnswered)
                .toList();
        if (turns.isEmpty()) {
            throw new ApiException(ErrorCode.SESSION_NOT_COMPLETABLE);
        }
        return new FeedbackContext(session, turns);
    }

    private AiFeedbackResponse generateFeedbackWithRetry(Session session, AiFeedbackRequest request) {
        ApiException lastException = null;
        for (int attempt = 0; attempt < MAX_FEEDBACK_RETRY; attempt++) {
            try {
                return aiConversationClient.generateFeedback(request);
            } catch (ApiException exception) {
                log.warn(
                        "AI 피드백 생성 시도가 실패했습니다. sessionId={} attempt={} maxAttempt={}",
                        session.getId(),
                        attempt + 1,
                        MAX_FEEDBACK_RETRY,
                        exception);
                lastException = exception;
            }
        }
        throw new ApiException(ErrorCode.FEEDBACK_GENERATION_FAILED, lastException == null
                ? ErrorCode.FEEDBACK_GENERATION_FAILED.getMessage()
                : lastException.getMessage());
    }

    private AiFeedbackRequest toAiFeedbackRequest(Session session, List<SessionTurn> turns) {
        return new AiFeedbackRequest(
                session.getScenario().getTitle(),
                session.getScenario().getAiRole(),
                session.getScenario().getSituation(),
                session.getScenario().getGoal(),
                session.getStatus().name(),
                toAiSlotStatuses(session),
                turns.stream()
                        .map(turn -> new AiFeedbackTurnRequest(turn.getId(), turn.getAiQuestion(), turn.getUserUtterance()))
                        .toList());
    }

    private List<AiSlotStatus> toAiSlotStatuses(Session session) {
        Map<String, String> slotDescriptionByName = scenarioSlotRepository.findByScenarioOrderByIdAsc(session.getScenario()).stream()
                .collect(Collectors.toMap(ScenarioSlot::getName, ScenarioSlot::getDescription));
        return slotStatusRepository.findBySessionOrderByIdAsc(session).stream()
                .map(slot -> new AiSlotStatus(
                        slot.getSlotName(),
                        slotDescriptionByName.get(slot.getSlotName()),
                        slot.isFulfilled()))
                .toList();
    }

    private void logStageLatency(String workflow, String stage, Long userId, Long sessionId, long startedAtNanos) {
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
        log.info(
                "event=be_stage_latency requestId={} workflow={} stage={} elapsedMs={} sessionId={} userId={}",
                RequestTraceContext.currentRequestId().orElse("none"),
                workflow,
                stage,
                elapsedMs,
                sessionId == null ? "none" : sessionId,
                userId);
    }

    private void relayFeedbackStream(FeedbackStreamContext context, OutputStream outputStream) throws IOException {
        boolean traceStarted = context.requestId() != null;
        if (traceStarted) {
            RequestTraceContext.start(context.requestId());
        }

        try {
            AiFeedbackSummaryData summary = null;
            Map<Long, AiTurnFeedbackResponse> turnFeedbacks = new LinkedHashMap<>();

            try (Stream<AiFeedbackStreamEvent> events = aiFeedbackStreamClient.streamFeedback(context.request()).toStream()) {
                Iterator<AiFeedbackStreamEvent> iterator = events.iterator();
                while (iterator.hasNext()) {
                    AiFeedbackStreamEvent event = iterator.next();
                    if ("summary".equals(event.event())) {
                        summary = handleSummaryEvent(event, outputStream, turnFeedbacks);
                        continue;
                    }
                    if ("turnFeedback".equals(event.event())) {
                        handleTurnFeedbackEvent(context, event, outputStream, summary, turnFeedbacks);
                        continue;
                    }
                    if ("done".equals(event.event())) {
                        handleDoneEvent(context, event, outputStream, summary, turnFeedbacks);
                        return;
                    }
                    if ("error".equals(event.event())) {
                        writeSseEvent(outputStream, event.event(), event.data());
                        return;
                    }
                    writeSseEvent(outputStream, event.event(), event.data());
                }
            } catch (AiFeedbackStreamException exception) {
                log.warn("AI 피드백 스트림 호출이 실패했습니다. sessionId={}", context.sessionId(), exception);
                writeBackendErrorEvent(outputStream, context.sessionId());
                return;
            } catch (RuntimeException exception) {
                log.warn("AI 피드백 스트림 처리 중 오류가 발생했습니다. sessionId={}", context.sessionId(), exception);
                writeBackendErrorEvent(outputStream, context.sessionId());
                return;
            }

            writeBackendErrorEvent(outputStream, context.sessionId());
        } finally {
            if (traceStarted) {
                RequestTraceContext.clear();
            }
        }
    }

    private AiFeedbackSummaryData handleSummaryEvent(
            AiFeedbackStreamEvent event,
            OutputStream outputStream,
            Map<Long, AiTurnFeedbackResponse> turnFeedbacks
    ) throws IOException {
        if (!turnFeedbacks.isEmpty()) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
        AiFeedbackSummaryData summary = readEventData(event.data(), AiFeedbackSummaryData.class);
        if (summary.comprehensionScore() == null
                || summary.comprehensionScore() < 0
                || summary.comprehensionScore() > 100
                || summary.feedbackSummary() == null
                || summary.feedbackSummary().isBlank()) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
        writeSseEvent(outputStream, event.event(), event.data());
        return summary;
    }

    private void handleTurnFeedbackEvent(
            FeedbackStreamContext context,
            AiFeedbackStreamEvent event,
            OutputStream outputStream,
            AiFeedbackSummaryData summary,
            Map<Long, AiTurnFeedbackResponse> turnFeedbacks
    ) throws IOException {
        if (summary == null) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
        AiFeedbackTurnData turnFeedback = readEventData(event.data(), AiFeedbackTurnData.class);
        if (turnFeedback.turnId() == null
                || turnFeedback.feedbackRequired() == null
                || !context.turnIds().contains(turnFeedback.turnId())
                || turnFeedbacks.containsKey(turnFeedback.turnId())) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
        turnFeedbacks.put(
                turnFeedback.turnId(),
                new AiTurnFeedbackResponse(
                        turnFeedback.turnId(),
                        turnFeedback.feedbackRequired(),
                        turnFeedback.nativeUnderstanding(),
                        turnFeedback.nativeLanguageInterpretation(),
                        turnFeedback.betterExpression()));
        writeSseEvent(outputStream, event.event(), event.data());
    }

    private void handleDoneEvent(
            FeedbackStreamContext context,
            AiFeedbackStreamEvent event,
            OutputStream outputStream,
            AiFeedbackSummaryData summary,
            Map<Long, AiTurnFeedbackResponse> turnFeedbacks
    ) throws IOException {
        AiFeedbackDoneData done = readEventData(event.data(), AiFeedbackDoneData.class);
        if (summary == null
                || done.turnCount() == null
                || done.turnCount() != context.turnIds().size()
                || turnFeedbacks.size() != context.turnIds().size()
                || !turnFeedbacks.keySet().containsAll(context.turnIds())) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
        persistStreamFeedback(context, summary, turnFeedbacks);
        log.info(
                "세션 피드백 스트림 생성을 완료했습니다. sessionId={} cleared={} comprehensionScore={} turnCount={}",
                context.sessionId(),
                context.cleared(),
                summary.comprehensionScore(),
                turnFeedbacks.size());
        writeSseEvent(outputStream, event.event(), event.data());
    }

    private void persistStreamFeedback(
            FeedbackStreamContext context,
            AiFeedbackSummaryData summary,
            Map<Long, AiTurnFeedbackResponse> aiFeedbackByTurnId
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            Session session = sessionRepository.findById(context.sessionId())
                    .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
            if (sessionFeedbackRepository.existsBySession(session)) {
                throw new ApiException(ErrorCode.SESSION_ALREADY_COMPLETED);
            }
            List<SessionTurn> turns = turnRepository.findBySessionOrderBySequenceAsc(session).stream()
                    .filter(SessionTurn::isAnswered)
                    .toList();
            boolean sameTurns = turns.stream().map(SessionTurn::getId).toList().equals(context.turnIds());
            if (!sameTurns) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
            }

            SessionFeedback sessionFeedback = sessionFeedbackRepository.save(new SessionFeedback(
                    session,
                    context.cleared(),
                    summary.comprehensionScore(),
                    summary.feedbackSummary()));
            turns.forEach(turn -> saveTurnFeedback(sessionFeedback, turn, aiFeedbackByTurnId.get(turn.getId())));
        });
    }

    private <T> T readEventData(String data, Class<T> dataType) {
        try {
            return objectMapper.readValue(data, dataType);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private void writeBackendErrorEvent(OutputStream outputStream, Long sessionId) throws IOException {
        writeSseEvent(outputStream, "error", objectMapper.writeValueAsString(objectMap(
                "sessionId", sessionId,
                "code", SSE_GENERATION_FAILED_CODE,
                "message", ErrorCode.FEEDBACK_GENERATION_FAILED.getMessage()
        )));
    }

    private void writeSseEvent(OutputStream outputStream, String eventName, String data) throws IOException {
        outputStream.write(("event: " + eventName + "\n").getBytes(StandardCharsets.UTF_8));
        String eventData = data == null ? "" : data;
        for (String line : eventData.split("\\R", -1)) {
            outputStream.write(("data: " + line + "\n").getBytes(StandardCharsets.UTF_8));
        }
        outputStream.write("\n".getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private Map<String, Object> objectMap(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            map.put((String) keyValues[index], keyValues[index + 1]);
        }
        return map;
    }

    private void validateAiFeedback(AiFeedbackResponse feedback, List<SessionTurn> turns) {
        if (feedback == null
                || feedback.feedbackSummary() == null
                || feedback.feedbackSummary().isBlank()
                || feedback.turnFeedbacks() == null
                || feedback.comprehensionScore() < 0
                || feedback.comprehensionScore() > 100) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
        Map<Long, AiTurnFeedbackResponse> feedbackByTurnId = feedback.turnFeedbacks().stream()
                .collect(Collectors.toMap(AiTurnFeedbackResponse::turnId, Function.identity()));
        boolean allTurnsHaveFeedback = turns.stream().allMatch(turn -> feedbackByTurnId.containsKey(turn.getId()));
        if (!allTurnsHaveFeedback) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private TurnFeedback saveTurnFeedback(
            SessionFeedback sessionFeedback,
            SessionTurn turn,
            AiTurnFeedbackResponse aiFeedback
    ) {
        return turnFeedbackRepository.save(new TurnFeedback(
                sessionFeedback,
                turn,
                aiFeedback.feedbackRequired(),
                aiFeedback.nativeUnderstanding(),
                aiFeedback.nativeLanguageInterpretation(),
                aiFeedback.betterExpression()));
    }

    private FeedbackResponse toResponse(
            Session session,
            SessionFeedback sessionFeedback,
            List<TurnFeedback> turnFeedbacks
    ) {
        return new FeedbackResponse(
                session.getId(),
                sessionFeedback.isCleared(),
                sessionFeedback.getComprehensionScore(),
                sessionFeedback.getFeedbackSummary(),
                session.getRemainingHearts(),
                turnFeedbacks.stream()
                        .map(this::toTurnFeedbackResponse)
                        .toList());
    }

    private TurnFeedbackResponse toTurnFeedbackResponse(TurnFeedback feedback) {
        SessionTurn turn = feedback.getTurn();
        return new TurnFeedbackResponse(
                turn.getId(),
                turn.getSequence(),
                turn.getAiQuestion(),
                turn.getTranslatedQuestion(),
                turn.getUserUtterance(),
                feedback.isFeedbackRequired(),
                feedback.getNativeUnderstanding(),
                feedback.getNativeLanguageInterpretation(),
                feedback.getBetterExpression());
    }

    private record FeedbackContext(Session session, List<SessionTurn> turns) {
    }

    private record FeedbackStreamContext(
            Long sessionId,
            boolean cleared,
            List<Long> turnIds,
            String requestId,
            AiFeedbackRequest request
    ) {
    }

    private record AiFeedbackSummaryData(
            Integer comprehensionScore,
            String feedbackSummary
    ) {
    }

    private record AiFeedbackTurnData(
            Long turnId,
            Boolean feedbackRequired,
            String nativeUnderstanding,
            String nativeLanguageInterpretation,
            String betterExpression
    ) {
    }

    private record AiFeedbackDoneData(Integer turnCount) {
    }
}

package com.saynow.practice.application;

import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.practice.api.dto.ExitSessionRequest;
import com.saynow.practice.api.dto.ExitSessionResponse;
import com.saynow.practice.api.dto.MicReadyRequest;
import com.saynow.practice.api.dto.MicReadyResponse;
import com.saynow.practice.api.dto.SessionStartResponse;
import com.saynow.practice.api.dto.SessionStatusResponse;
import com.saynow.practice.api.dto.StartSessionRequest;
import com.saynow.practice.api.dto.SubmitTurnRequest;
import com.saynow.practice.api.dto.TurnHistoryResponse;
import com.saynow.practice.api.dto.TurnSubmitResponse;
import com.saynow.practice.domain.InputType;
import com.saynow.practice.domain.PracticeSession;
import com.saynow.practice.domain.PracticeTurn;
import com.saynow.practice.domain.PromptType;
import com.saynow.practice.domain.SessionMetric;
import com.saynow.practice.domain.SessionPrompt;
import com.saynow.practice.domain.SessionSlotValue;
import com.saynow.practice.domain.SessionStatus;
import com.saynow.practice.infrastructure.PracticeSessionRepository;
import com.saynow.practice.infrastructure.PracticeTurnRepository;
import com.saynow.practice.infrastructure.SessionMetricRepository;
import com.saynow.practice.infrastructure.SessionPromptRepository;
import com.saynow.practice.infrastructure.SessionSlotValueRepository;
import com.saynow.practice.infrastructure.ai.AiFilledSlot;
import com.saynow.practice.infrastructure.ai.AiPrompt;
import com.saynow.practice.infrastructure.ai.AiTurnEvaluationRequest;
import com.saynow.practice.infrastructure.ai.AiTurnEvaluationResult;
import com.saynow.practice.infrastructure.ai.LocalAiPracticeClient;
import com.saynow.scenario.domain.ContentStatus;
import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.ScenarioSlot;
import com.saynow.scenario.infrastructure.ScenarioRepository;
import com.saynow.scenario.infrastructure.ScenarioSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PracticeSessionService {

    private static final long MAX_AUDIO_BYTES = 10 * 1024 * 1024;
    private static final Set<String> SUPPORTED_AUDIO_CONTENT_TYPES = Set.of(
            "audio/webm",
            "audio/wav",
            "audio/x-wav",
            "audio/mpeg",
            "audio/mp4",
            "audio/x-m4a"
    );

    private final ScenarioRepository scenarioRepository;
    private final ScenarioSlotRepository scenarioSlotRepository;
    private final PracticeSessionRepository sessionRepository;
    private final SessionPromptRepository promptRepository;
    private final SessionMetricRepository metricRepository;
    private final PracticeTurnRepository turnRepository;
    private final SessionSlotValueRepository slotValueRepository;
    private final LocalAiPracticeClient aiPracticeClient;
    private final FeedbackCreationService feedbackCreationService;

    public PracticeSessionService(
            ScenarioRepository scenarioRepository,
            ScenarioSlotRepository scenarioSlotRepository,
            PracticeSessionRepository sessionRepository,
            SessionPromptRepository promptRepository,
            SessionMetricRepository metricRepository,
            PracticeTurnRepository turnRepository,
            SessionSlotValueRepository slotValueRepository,
            LocalAiPracticeClient aiPracticeClient,
            FeedbackCreationService feedbackCreationService
    ) {
        this.scenarioRepository = scenarioRepository;
        this.scenarioSlotRepository = scenarioSlotRepository;
        this.sessionRepository = sessionRepository;
        this.promptRepository = promptRepository;
        this.metricRepository = metricRepository;
        this.turnRepository = turnRepository;
        this.slotValueRepository = slotValueRepository;
        this.aiPracticeClient = aiPracticeClient;
        this.feedbackCreationService = feedbackCreationService;
    }

    @Transactional
    public SessionStartResponse startSession(StartSessionRequest request) {
        Scenario scenario = scenarioRepository.findByScenarioKeyAndStatus(request.scenarioId(), ContentStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.SCENARIO_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        PracticeSession session = sessionRepository.save(new PracticeSession(UUID.randomUUID().toString(), scenario, now));
        SessionPrompt openingPrompt = promptRepository.save(new SessionPrompt(
                session,
                1,
                PromptType.OPENING,
                scenario.getOpeningBabsaeText(),
                scenario.getOpeningTtsUrl(),
                now));

        return new SessionStartResponse(
                session.getPublicId(),
                scenario.getScenarioKey(),
                session.getStatus(),
                openingPrompt.getPromptText(),
                openingPrompt.getTtsUrl(),
                0,
                session.getMaxFollowUpCount(),
                session.getStartedAt());
    }

    @Transactional
    public MicReadyResponse recordMicReady(String sessionId, MicReadyRequest request) {
        PracticeSession session = findSession(sessionId);
        assertInProgress(session);

        LocalDateTime now = LocalDateTime.now();
        SessionMetric metric = metricRepository.findBySessionAndMetricKey(session, SessionMetric.MIC_READY_LATENCY_MS)
                .map(existing -> {
                    existing.update(request.latencyMs(), now);
                    return existing;
                })
                .orElseGet(() -> new SessionMetric(
                        session,
                        SessionMetric.MIC_READY_LATENCY_MS,
                        request.latencyMs(),
                        SessionMetric.UNIT_MS,
                        now));
        metricRepository.save(metric);

        return new MicReadyResponse(session.getPublicId(), metric.getMetricValue());
    }

    @Transactional
    public TurnSubmitResponse submitTurn(String sessionId, SubmittedAudio audio, SubmitTurnRequest request) {
        PracticeSession session = findSession(sessionId);
        assertInProgress(session);
        validateTurnSubmitRequest(audio, request);

        LocalDateTime now = LocalDateTime.now();
        SessionPrompt currentPrompt = latestPrompt(session);
        int turnIndex = Math.toIntExact(turnRepository.countBySession(session) + 1);
        long followUpCountBeforeEvaluation = followUpCount(session);
        List<ScenarioSlot> scenarioSlots = scenarioSlotRepository.findByScenarioOrderBySlotOrderAsc(session.getScenario());
        Set<String> currentFilledSlotKeys = slotValueRepository.findBySession(session).stream()
                .map(value -> value.getScenarioSlot().getSlotKey())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        AiTurnEvaluationResult evaluation = aiPracticeClient.evaluateTurn(new AiTurnEvaluationRequest(
                audio.filename(),
                audio.contentType(),
                audio.content(),
                request.inputType(),
                request.speechStartedAfterMs(),
                request.recordingDurationMs(),
                scenarioSlots,
                currentFilledSlotKeys,
                followUpCountBeforeEvaluation,
                session.getMaxFollowUpCount()));
        validateAiEvaluationResult(evaluation);

        PracticeTurn turn = turnRepository.save(new PracticeTurn(
                session,
                currentPrompt,
                turnIndex,
                evaluation.transcript().trim(),
                request.inputType(),
                request.speechStartedAfterMs(),
                request.recordingDurationMs(),
                evaluation.sttConfidence(),
                now));

        Set<String> filledAfterTurn = saveFilledSlots(session, turn, scenarioSlots, evaluation.filledSlots(), currentFilledSlotKeys, now);
        SessionStatus nextStatus = decideNextStatus(session, scenarioSlots, filledAfterTurn, followUpCountBeforeEvaluation, evaluation);
        SessionPrompt responsePrompt = appendPromptForStatus(session, currentPrompt, nextStatus, evaluation, now);

        boolean feedbackAvailable = false;
        if (nextStatus == SessionStatus.SUCCESS || nextStatus == SessionStatus.FAILURE) {
            session.finish(nextStatus, now, null);
            feedbackCreationService.createReadyFeedback(session, now);
            feedbackAvailable = true;
        }

        return new TurnSubmitResponse(
                session.getPublicId(),
                turn.getId(),
                turn.getTurnIndex(),
                turn.getUserTranscript(),
                evaluation.sttConfidence(),
                session.getStatus(),
                responsePrompt.getPromptText(),
                responsePrompt.getTtsUrl(),
                followUpCount(session),
                session.getMaxFollowUpCount(),
                feedbackAvailable);
    }

    @Transactional(readOnly = true)
    public SessionStatusResponse getSession(String sessionId) {
        PracticeSession session = findSession(sessionId);
        SessionPrompt latestPrompt = latestPrompt(session);
        Long micReadyLatencyMs = metricRepository.findBySessionAndMetricKey(session, SessionMetric.MIC_READY_LATENCY_MS)
                .map(SessionMetric::getMetricValue)
                .orElse(null);
        List<TurnHistoryResponse> turns = turnRepository.findBySessionOrderByTurnIndexAsc(session).stream()
                .map(turn -> new TurnHistoryResponse(
                        turn.getId(),
                        turn.getTurnIndex(),
                        turn.getPrompt().getPromptText(),
                        turn.getUserTranscript(),
                        turn.getSpeechStartedAfterMs(),
                        turn.getRecordingDurationMs(),
                        turn.getCreatedAt()))
                .toList();

        return new SessionStatusResponse(
                session.getPublicId(),
                session.getScenario().getScenarioKey(),
                session.getStatus(),
                latestPrompt.getPromptText(),
                latestPrompt.getTtsUrl(),
                followUpCount(session),
                session.getMaxFollowUpCount(),
                micReadyLatencyMs,
                turns);
    }

    @Transactional
    public ExitSessionResponse exitSession(String sessionId, ExitSessionRequest request) {
        PracticeSession session = findSession(sessionId);
        assertInProgress(session);

        LocalDateTime now = LocalDateTime.now();
        session.finish(SessionStatus.ABANDONED, now, request.reason());
        return new ExitSessionResponse(session.getPublicId(), session.getStatus(), session.getEndedAt());
    }

    private PracticeSession findSession(String sessionId) {
        return sessionRepository.findByPublicId(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    }

    private void assertInProgress(PracticeSession session) {
        if (!session.isInProgress()) {
            throw new ApiException(ErrorCode.SESSION_ALREADY_ENDED);
        }
    }

    private SessionPrompt latestPrompt(PracticeSession session) {
        return promptRepository.findFirstBySessionOrderByPromptIndexDesc(session)
                .orElseThrow(() -> new ApiException(ErrorCode.PROMPT_NOT_FOUND));
    }

    private long followUpCount(PracticeSession session) {
        return promptRepository.countBySessionAndPromptType(session, PromptType.FOLLOW_UP);
    }

    private void validateTurnSubmitRequest(SubmittedAudio audio, SubmitTurnRequest request) {
        if (request.inputType() != InputType.AUDIO) {
            throw new ApiException(ErrorCode.UNSUPPORTED_INPUT_TYPE);
        }
        if (audio == null || audio.size() <= 0 || audio.content() == null || audio.content().length == 0) {
            throw new ApiException(ErrorCode.AUDIO_REQUIRED);
        }
        if (audio.size() > MAX_AUDIO_BYTES) {
            throw new ApiException(ErrorCode.AUDIO_TOO_LARGE);
        }
        if (audio.contentType() == null || !SUPPORTED_AUDIO_CONTENT_TYPES.contains(audio.contentType())) {
            throw new ApiException(ErrorCode.UNSUPPORTED_AUDIO_TYPE);
        }
    }

    private void validateAiEvaluationResult(AiTurnEvaluationResult evaluation) {
        if (evaluation.transcript() == null || evaluation.transcript().isBlank()) {
            throw new ApiException(ErrorCode.AI_STT_FAILED);
        }
        if (evaluation.sttConfidence() != null
                && (evaluation.sttConfidence().signum() < 0 || evaluation.sttConfidence().compareTo(java.math.BigDecimal.ONE) > 0)) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private Set<String> saveFilledSlots(
            PracticeSession session,
            PracticeTurn turn,
            List<ScenarioSlot> scenarioSlots,
            List<AiFilledSlot> aiFilledSlots,
            Set<String> currentFilledSlotKeys,
            LocalDateTime now
    ) {
        Map<String, ScenarioSlot> slotsByKey = scenarioSlots.stream()
                .collect(Collectors.toMap(ScenarioSlot::getSlotKey, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Set<String> filledAfterTurn = new LinkedHashSet<>(currentFilledSlotKeys);

        for (AiFilledSlot aiFilledSlot : aiFilledSlots) {
            ScenarioSlot scenarioSlot = slotsByKey.get(aiFilledSlot.slotKey());
            if (scenarioSlot == null || filledAfterTurn.contains(aiFilledSlot.slotKey())) {
                continue;
            }
            if (!slotValueRepository.existsBySessionAndScenarioSlot(session, scenarioSlot)) {
                slotValueRepository.save(new SessionSlotValue(session, scenarioSlot, turn, aiFilledSlot.slotValue(), now));
            }
            filledAfterTurn.add(aiFilledSlot.slotKey());
        }
        return filledAfterTurn;
    }

    private SessionStatus decideNextStatus(
            PracticeSession session,
            List<ScenarioSlot> scenarioSlots,
            Set<String> filledAfterTurn,
            long followUpCountBeforeEvaluation,
            AiTurnEvaluationResult evaluation
    ) {
        boolean allRequiredSlotsFilled = scenarioSlots.stream()
                .filter(ScenarioSlot::isRequired)
                .allMatch(slot -> filledAfterTurn.contains(slot.getSlotKey()));
        if (allRequiredSlotsFilled) {
            return SessionStatus.SUCCESS;
        }
        if (followUpCountBeforeEvaluation >= session.getMaxFollowUpCount()) {
            return SessionStatus.FAILURE;
        }
        if (evaluation.scenarioStatus() == SessionStatus.FAILURE) {
            return SessionStatus.FAILURE;
        }
        return SessionStatus.IN_PROGRESS;
    }

    private SessionPrompt appendPromptForStatus(
            PracticeSession session,
            SessionPrompt currentPrompt,
            SessionStatus nextStatus,
            AiTurnEvaluationResult evaluation,
            LocalDateTime now
    ) {
        if (nextStatus == SessionStatus.IN_PROGRESS) {
            AiPrompt nextPrompt = evaluation.nextPrompt();
            if (nextPrompt == null) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "다음 질문을 생성할 수 없습니다.");
            }
            return promptRepository.save(new SessionPrompt(
                    session,
                    currentPrompt.getPromptIndex() + 1,
                    PromptType.FOLLOW_UP,
                    nextPrompt.promptText(),
                    nextPrompt.ttsUrl(),
                    now));
        }

        AiPrompt resultPrompt = evaluation.resultMessage();
        String promptText = nextStatus == SessionStatus.SUCCESS ? "Scenario cleared." : "The scenario was not cleared in time.";
        String ttsUrl = null;
        if (resultPrompt != null) {
            promptText = resultPrompt.promptText();
            ttsUrl = resultPrompt.ttsUrl();
        }
        return promptRepository.save(new SessionPrompt(
                session,
                currentPrompt.getPromptIndex() + 1,
                PromptType.RESULT,
                promptText,
                ttsUrl,
                now));
    }
}

package com.saynow.practice.application;

import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
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
import com.saynow.practice.domain.SessionStatus;
import com.saynow.practice.infrastructure.PracticeSessionRepository;
import com.saynow.practice.infrastructure.PracticeTurnRepository;
import com.saynow.practice.infrastructure.ai.AiFilledSlot;
import com.saynow.practice.infrastructure.ai.AiPrompt;
import com.saynow.practice.infrastructure.ai.AiTurnEvaluationRequest;
import com.saynow.practice.infrastructure.ai.AiTurnEvaluationResult;
import com.saynow.practice.infrastructure.ai.LocalAiPracticeClient;
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
    private final PracticeTurnRepository turnRepository;
    private final LocalAiPracticeClient aiPracticeClient;
    private final FeedbackCreationService feedbackCreationService;

    public PracticeSessionService(
            ScenarioRepository scenarioRepository,
            ScenarioSlotRepository scenarioSlotRepository,
            PracticeSessionRepository sessionRepository,
            PracticeTurnRepository turnRepository,
            LocalAiPracticeClient aiPracticeClient,
            FeedbackCreationService feedbackCreationService
    ) {
        this.scenarioRepository = scenarioRepository;
        this.scenarioSlotRepository = scenarioSlotRepository;
        this.sessionRepository = sessionRepository;
        this.turnRepository = turnRepository;
        this.aiPracticeClient = aiPracticeClient;
        this.feedbackCreationService = feedbackCreationService;
    }

    @Transactional
    public SessionStartResponse startSession(StartSessionRequest request) {
        Scenario scenario = scenarioRepository.findByScenarioKey(request.scenarioId())
                .orElseThrow(() -> new ApiException(ErrorCode.SCENARIO_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        PracticeSession session = sessionRepository.save(new PracticeSession(UUID.randomUUID().toString(), scenario, now));

        return new SessionStartResponse(
                session.getPublicId(),
                scenario.getScenarioKey(),
                session.getStatus(),
                session.getCurrentBabsaeText(),
                session.getCurrentBabsaeTtsUrl(),
                session.getFollowUpCount(),
                scenario.getMaxFollowUpCount(),
                session.getStartedAt());
    }

    @Transactional
    public MicReadyResponse recordMicReady(String sessionId, MicReadyRequest request) {
        PracticeSession session = findSession(sessionId);
        assertInProgress(session);

        session.recordMicReady(request.latencyMs());
        return new MicReadyResponse(session.getPublicId(), session.getMicReadyLatencyMs());
    }

    @Transactional
    public TurnSubmitResponse submitTurn(String sessionId, SubmittedAudio audio, SubmitTurnRequest request) {
        PracticeSession session = findSession(sessionId);
        assertInProgress(session);
        validateTurnSubmitRequest(audio, request);

        List<ScenarioSlot> scenarioSlots = scenarioSlotRepository.findByScenarioOrderByIdAsc(session.getScenario());
        Map<String, String> currentFilledSlots = new LinkedHashMap<>(session.getFilledSlots());
        int turnIndex = Math.toIntExact(turnRepository.countBySession(session) + 1);

        AiTurnEvaluationResult evaluation = aiPracticeClient.evaluateTurn(new AiTurnEvaluationRequest(
                session.getPublicId(),
                session.getScenario(),
                session.getCurrentBabsaeText(),
                session.getCurrentBabsaeTtsUrl(),
                audio.filename(),
                audio.contentType(),
                audio.content(),
                request.inputType(),
                request.speechStartedAfterMs(),
                request.recordingDurationMs(),
                scenarioSlots,
                currentFilledSlots,
                session.getFollowUpCount(),
                session.getScenario().getMaxFollowUpCount()));
        validateAiEvaluationResult(evaluation);

        LocalDateTime now = LocalDateTime.now();
        PracticeTurn turn = turnRepository.save(new PracticeTurn(
                session,
                turnIndex,
                session.getCurrentBabsaeText(),
                session.getCurrentBabsaeTtsUrl(),
                evaluation.transcript().trim(),
                request.inputType(),
                request.speechStartedAfterMs(),
                request.recordingDurationMs(),
                evaluation.sttConfidence()));

        Set<String> filledAfterTurn = saveFilledSlots(session, scenarioSlots, evaluation.filledSlots());
        SessionStatus nextStatus = decideNextStatus(session, scenarioSlots, filledAfterTurn, evaluation);
        boolean feedbackAvailable = applyNextStatus(session, nextStatus, evaluation, now);

        return new TurnSubmitResponse(
                session.getPublicId(),
                turn.getId(),
                turn.getTurnIndex(),
                turn.getUserTranscript(),
                turn.getSttConfidence(),
                session.getStatus(),
                session.getCurrentBabsaeText(),
                session.getCurrentBabsaeTtsUrl(),
                session.getFollowUpCount(),
                session.getScenario().getMaxFollowUpCount(),
                feedbackAvailable);
    }

    @Transactional(readOnly = true)
    public SessionStatusResponse getSession(String sessionId) {
        PracticeSession session = findSession(sessionId);
        List<TurnHistoryResponse> turns = turnRepository.findBySessionOrderByTurnIndexAsc(session).stream()
                .map(turn -> new TurnHistoryResponse(
                        turn.getId(),
                        turn.getTurnIndex(),
                        turn.getQuestionText(),
                        turn.getUserTranscript(),
                        turn.getSpeechStartedAfterMs(),
                        turn.getRecordingDurationMs(),
                        turn.getCreatedAt()))
                .toList();

        return new SessionStatusResponse(
                session.getPublicId(),
                session.getScenario().getScenarioKey(),
                session.getStatus(),
                session.getCurrentBabsaeText(),
                session.getCurrentBabsaeTtsUrl(),
                session.getFollowUpCount(),
                session.getScenario().getMaxFollowUpCount(),
                session.getMicReadyLatencyMs(),
                turns);
    }

    @Transactional
    public ExitSessionResponse exitSession(String sessionId) {
        PracticeSession session = findSession(sessionId);
        assertInProgress(session);

        LocalDateTime now = LocalDateTime.now();
        session.abandon(now);
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
        if (evaluation.scenarioStatus() == null || evaluation.filledSlots() == null) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private Set<String> saveFilledSlots(
            PracticeSession session,
            List<ScenarioSlot> scenarioSlots,
            List<AiFilledSlot> aiFilledSlots
    ) {
        Map<String, ScenarioSlot> slotsByKey = scenarioSlots.stream()
                .collect(Collectors.toMap(ScenarioSlot::getSlotKey, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Set<String> filledAfterTurn = new LinkedHashSet<>(session.getFilledSlots().keySet());

        for (AiFilledSlot aiFilledSlot : aiFilledSlots) {
            if (!slotsByKey.containsKey(aiFilledSlot.slotKey()) || filledAfterTurn.contains(aiFilledSlot.slotKey())) {
                continue;
            }
            if (aiFilledSlot.slotValue() == null || aiFilledSlot.slotValue().isBlank()) {
                continue;
            }
            session.putFilledSlot(aiFilledSlot.slotKey(), aiFilledSlot.slotValue());
            filledAfterTurn.add(aiFilledSlot.slotKey());
        }
        return filledAfterTurn;
    }

    private SessionStatus decideNextStatus(
            PracticeSession session,
            List<ScenarioSlot> scenarioSlots,
            Set<String> filledAfterTurn,
            AiTurnEvaluationResult evaluation
    ) {
        boolean allRequiredSlotsFilled = scenarioSlots.stream()
                .allMatch(slot -> filledAfterTurn.contains(slot.getSlotKey()));
        if (allRequiredSlotsFilled) {
            return SessionStatus.SUCCESS;
        }
        if (session.getFollowUpCount() >= session.getScenario().getMaxFollowUpCount()) {
            return SessionStatus.FAILURE;
        }
        if (evaluation.scenarioStatus() == SessionStatus.FAILURE) {
            return SessionStatus.FAILURE;
        }
        return SessionStatus.IN_PROGRESS;
    }

    private boolean applyNextStatus(
            PracticeSession session,
            SessionStatus nextStatus,
            AiTurnEvaluationResult evaluation,
            LocalDateTime now
    ) {
        if (nextStatus == SessionStatus.IN_PROGRESS) {
            AiPrompt nextQuestion = evaluation.nextPrompt();
            if (nextQuestion == null || nextQuestion.text() == null || nextQuestion.text().isBlank()) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "다음 질문을 생성할 수 없습니다.");
            }
            session.applyFollowUp(nextQuestion.text(), nextQuestion.ttsUrl());
            return false;
        }

        AiPrompt resultMessage = evaluation.resultMessage();
        String messageText = nextStatus == SessionStatus.SUCCESS ? "Scenario cleared." : "The scenario was not cleared in time.";
        String ttsUrl = null;
        if (resultMessage != null && resultMessage.text() != null && !resultMessage.text().isBlank()) {
            messageText = resultMessage.text();
            ttsUrl = resultMessage.ttsUrl();
        }
        session.finish(nextStatus, messageText, ttsUrl, now);
        feedbackCreationService.createReadyFeedback(session);
        return true;
    }
}

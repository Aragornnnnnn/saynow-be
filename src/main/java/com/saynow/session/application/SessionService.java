// 2차 MVP 시나리오 세션 시작, 발화 제출, 중도 종료를 처리하는 서비스
package com.saynow.session.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.auth.domain.User;
import com.saynow.auth.infrastructure.UserRepository;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.common.observability.RequestTraceContext;
import com.saynow.session.api.dto.GuideQuestionRequest;
import com.saynow.session.api.dto.GuideQuestionResponse;
import com.saynow.session.api.dto.SessionResultResponse;
import com.saynow.session.api.dto.SessionStartResponse;
import com.saynow.session.api.dto.UserUtteranceRequest;
import com.saynow.session.api.dto.UserUtteranceResponse;
import com.saynow.session.domain.Session;
import com.saynow.session.domain.SessionTurn;
import com.saynow.session.domain.SessionSlotStatus;
import com.saynow.session.domain.SessionStatus;
import com.saynow.session.infrastructure.SessionRepository;
import com.saynow.session.infrastructure.SessionRepository.SubmitUtteranceContextRow;
import com.saynow.session.infrastructure.SessionTurnRepository;
import com.saynow.session.infrastructure.SessionSlotStatusRepository;
import com.saynow.session.infrastructure.ai.AiConversationClient;
import com.saynow.session.infrastructure.ai.AiFilledSlot;
import com.saynow.session.infrastructure.ai.AiGuideRequest;
import com.saynow.session.infrastructure.ai.AiGuideResponse;
import com.saynow.session.infrastructure.ai.AiNextQuestionRequest;
import com.saynow.session.infrastructure.ai.AiNextQuestionResponse;
import com.saynow.session.infrastructure.ai.AiNextQuestionSlotStatus;
import com.saynow.session.infrastructure.ai.AiSlotEvidencePolicy;
import com.saynow.session.infrastructure.ai.TurnClassification;
import com.saynow.scenario.application.ScenarioService;
import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.UserScenarioProgress;
import com.saynow.scenario.infrastructure.ScenarioRepository;
import com.saynow.scenario.infrastructure.ScenarioSlotRepository;
import com.saynow.scenario.infrastructure.UserScenarioProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private static final String OPEN_CATEGORY_NAME = "Airport";

    private final ScenarioRepository scenarioRepository;
    private final ScenarioSlotRepository scenarioSlotRepository;
    private final SessionRepository sessionRepository;
    private final SessionTurnRepository turnRepository;
    private final SessionSlotStatusRepository slotStatusRepository;
    private final UserScenarioProgressRepository userScenarioProgressRepository;
    private final UserRepository userRepository;
    private final AiConversationClient aiConversationClient;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public SessionStartResponse startSession(Long userId, Long scenarioId) {
        long stageStartedAt = System.nanoTime();
        User user = findUser(userId);
        logStageLatency("session_start", "load_user", userId, null, stageStartedAt);

        stageStartedAt = System.nanoTime();
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ApiException(ErrorCode.SCENARIO_NOT_FOUND));
        logStageLatency("session_start", "load_scenario", userId, null, stageStartedAt);

        stageStartedAt = System.nanoTime();
        assertPlayable(user, scenario);
        logStageLatency("session_start", "assert_playable", userId, null, stageStartedAt);

        stageStartedAt = System.nanoTime();
        userScenarioProgressRepository.findByUserAndScenario(user, scenario)
                .orElseGet(() -> userScenarioProgressRepository.save(new UserScenarioProgress(user, scenario)));
        logStageLatency("session_start", "ensure_progress", userId, null, stageStartedAt);

        stageStartedAt = System.nanoTime();
        Session session = sessionRepository.save(new Session(user, scenario));
        logStageLatency("session_start", "save_session", userId, session.getId(), stageStartedAt);

        stageStartedAt = System.nanoTime();
        turnRepository.save(new SessionTurn(session, 1, scenario.getOriginalQuestion(), scenario.getTranslatedQuestion()));
        logStageLatency("session_start", "save_initial_turn", userId, session.getId(), stageStartedAt);

        stageStartedAt = System.nanoTime();
        scenarioSlotRepository.findByScenarioOrderByIdAsc(scenario)
                .forEach(slot -> slotStatusRepository.save(new SessionSlotStatus(session, slot.getName())));
        logStageLatency("session_start", "save_slot_statuses", userId, session.getId(), stageStartedAt);

        log.info("세션을 시작했습니다. userId={} scenarioId={} sessionId={}", userId, scenarioId, session.getId());
        return new SessionStartResponse(
                session.getId(),
                scenario.getOriginalQuestion(),
                scenario.getTranslatedQuestion(),
                session.getRemainingHearts(),
                false);
    }

    public UserUtteranceResponse submitUtterance(Long userId, Long sessionId, UserUtteranceRequest request) {
        validateUserUtterance(request);
        String userUtterance = request.userUtterance().trim();

        long stageStartedAt = System.nanoTime();
        SubmitUtteranceContext context = Objects.requireNonNull(transactionTemplate.execute(status -> {
            SubmitUtteranceContext loadedContext = loadSubmitUtteranceContext(userId, sessionId);
            assertInProgress(loadedContext.sessionStatus());
            if (loadedContext.currentTurnId() == null) {
                throw new ApiException(ErrorCode.SESSION_NOT_COMPLETABLE);
            }
            return loadedContext;
        }));
        logStageLatency("submit_utterance", "load_context", userId, sessionId, stageStartedAt);

        stageStartedAt = System.nanoTime();
        AiNextQuestionRequest aiRequest = toAiNextQuestionRequest(context, userUtterance);
        logStageLatency("submit_utterance", "prepare_ai_request", userId, sessionId, stageStartedAt);

        AiNextQuestionResponse aiResponse = aiConversationClient.generateNextQuestion(aiRequest);
        validateNextQuestionResponse(aiResponse);

        return Objects.requireNonNull(transactionTemplate.execute(status ->
                persistUtteranceResult(userId, sessionId, userUtterance, context, aiResponse)));
    }

    private UserUtteranceResponse persistUtteranceResult(
            Long userId,
            Long sessionId,
            String userUtterance,
            SubmitUtteranceContext context,
            AiNextQuestionResponse aiResponse
    ) {
        long stageStartedAt = System.nanoTime();
        int updatedTurnCount = turnRepository.updateUserUtteranceIfPending(context.currentTurnId(), userUtterance);
        if (updatedTurnCount == 0) {
            throw new ApiException(ErrorCode.SESSION_NOT_COMPLETABLE);
        }
        logStageLatency("submit_utterance", "record_user_utterance", userId, sessionId, stageStartedAt);

        stageStartedAt = System.nanoTime();
        boolean heartDeducted = shouldDeductHeart(context.remainingHearts(), aiResponse);
        int remainingHearts = heartDeducted ? context.remainingHearts() - 1 : context.remainingHearts();
        Set<String> newlyFilledSlotNames = Set.of();
        if (aiResponse.turnClassification() == TurnClassification.ANSWER) {
            newlyFilledSlotNames = newlyFilledSlotNames(context.slotStatuses(), aiResponse.filledSlots());
            if (!newlyFilledSlotNames.isEmpty()) {
                slotStatusRepository.fulfillBySessionIdAndSlotNameIn(sessionId, newlyFilledSlotNames);
            }
        }
        logStageLatency("submit_utterance", "apply_ai_response", userId, sessionId, stageStartedAt);

        if (allFulfilled(context.slotStatuses(), newlyFilledSlotNames)) {
            stageStartedAt = System.nanoTime();
            sessionRepository.updateCompletion(sessionId, remainingHearts, SessionStatus.SUCCESS, LocalDateTime.now());
            userScenarioProgressRepository.markClearedByUserIdAndScenarioId(userId, context.scenarioId());
            logStageLatency("submit_utterance", "complete_success", userId, sessionId, stageStartedAt);
            logUtteranceProcessed(userId, context, aiResponse, heartDeducted, remainingHearts, SessionStatus.SUCCESS);
            return completedResponse(context.sessionId(), remainingHearts, heartDeducted, aiResponse.turnClassification());
        }
        if (remainingHearts <= 0) {
            stageStartedAt = System.nanoTime();
            sessionRepository.updateCompletion(sessionId, remainingHearts, SessionStatus.FAILURE, LocalDateTime.now());
            logStageLatency("submit_utterance", "complete_failure", userId, sessionId, stageStartedAt);
            logUtteranceProcessed(userId, context, aiResponse, heartDeducted, remainingHearts, SessionStatus.FAILURE);
            return completedResponse(context.sessionId(), remainingHearts, heartDeducted, aiResponse.turnClassification());
        }
        if (heartDeducted) {
            sessionRepository.updateRemainingHearts(sessionId, remainingHearts);
        }
        if (aiResponse.nextQuestion() == null || aiResponse.nextQuestion().isBlank()
                || aiResponse.translatedQuestion() == null || aiResponse.translatedQuestion().isBlank()) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
        String nextQuestionTargetSlotName = validNextQuestionTargetSlotName(
                aiResponse.nextQuestionTargetSlotName(),
                context.slotStatuses(),
                newlyFilledSlotNames);

        stageStartedAt = System.nanoTime();
        turnRepository.save(new SessionTurn(
                sessionRepository.getReferenceById(sessionId),
                context.currentTurnSequence() + 1,
                aiResponse.nextQuestion(),
                aiResponse.translatedQuestion(),
                nextQuestionTargetSlotName));
        logStageLatency("submit_utterance", "save_next_turn", userId, sessionId, stageStartedAt);
        logUtteranceProcessed(userId, context, aiResponse, heartDeducted, remainingHearts, SessionStatus.IN_PROGRESS);
        return new UserUtteranceResponse(
                context.sessionId(),
                aiResponse.nextQuestion(),
                aiResponse.translatedQuestion(),
                remainingHearts,
                false,
                heartDeducted,
                aiResponse.turnClassification());
    }

    @Transactional(readOnly = true)
    public GuideQuestionResponse generateGuideAnswer(Long userId, Long sessionId, GuideQuestionRequest request) {
        Session session = findOwnedSession(userId, sessionId);
        assertInProgress(session);
        String question = validateGuideQuestion(request);

        AiGuideResponse aiResponse = aiConversationClient.generateGuide(new AiGuideRequest(
                question,
                session.getScenario().getTitle(),
                session.getScenario().getGoal(),
                session.getScenario().getSituation(),
                session.getScenario().getAiRole()));
        if (aiResponse == null || aiResponse.answer() == null || aiResponse.answer().isBlank()) {
            throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
        }
        log.info("가이드 답변을 생성했습니다. userId={} sessionId={}", userId, sessionId);
        return new GuideQuestionResponse(aiResponse.answer().trim());
    }

    @Transactional(readOnly = true)
    public SessionResultResponse getSessionResult(Long userId, Long sessionId) {
        Session session = findOwnedSession(userId, sessionId);
        if (session.getStatus() != SessionStatus.SUCCESS && session.getStatus() != SessionStatus.FAILURE) {
            throw new ApiException(ErrorCode.SESSION_NOT_COMPLETABLE);
        }
        return new SessionResultResponse(session.getStatus().name());
    }

    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        Session session = findOwnedSession(userId, sessionId);
        if (!session.isInProgress()) {
            throw new ApiException(ErrorCode.SESSION_ALREADY_COMPLETED);
        }
        sessionRepository.delete(session);
        log.info("세션을 삭제했습니다. userId={} sessionId={}", userId, sessionId);
    }

    private User findUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_REQUIRED));
    }

    private Session findOwnedSession(Long userId, Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
        if (!session.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return session;
    }

    private void assertInProgress(Session session) {
        if (!session.isInProgress()) {
            throw new ApiException(ErrorCode.SESSION_ALREADY_COMPLETED);
        }
    }

    private void assertInProgress(SessionStatus status) {
        if (status != SessionStatus.IN_PROGRESS) {
            throw new ApiException(ErrorCode.SESSION_ALREADY_COMPLETED);
        }
    }

    private void validateUserUtterance(UserUtteranceRequest request) {
        if (request == null || request.userUtterance() == null || request.userUtterance().isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String validateGuideQuestion(GuideQuestionRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return request.question().trim();
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

    private void assertPlayable(User user, Scenario scenario) {
        if (!OPEN_CATEGORY_NAME.equals(scenario.getCategory().getName())) {
            throw new ApiException(ErrorCode.CATEGORY_LOCKED);
        }

        List<Scenario> scenarios = scenarioRepository.findByCategoryOrderByDisplayOrderAsc(scenario.getCategory());
        Map<Long, Boolean> clearedByScenarioId = userScenarioProgressRepository.findByUserIdAndScenarioIdIn(
                        user.getId(),
                        scenarios.stream().map(Scenario::getId).toList())
                .stream()
                .collect(Collectors.toMap(progress -> progress.getScenario().getId(), UserScenarioProgress::isCleared));

        boolean previousCleared = true;
        for (Scenario candidate : scenarios) {
            if (candidate.getId().equals(scenario.getId())) {
                if (!previousCleared) {
                    throw new ApiException(ErrorCode.SCENARIO_LOCKED, ScenarioService.PREVIOUS_SCENARIO_NOT_CLEARED);
                }
                return;
            }
            previousCleared = clearedByScenarioId.getOrDefault(candidate.getId(), false);
        }
    }

    private void validateNextQuestionResponse(AiNextQuestionResponse response) {
        if (response == null || response.filledSlots() == null || response.turnClassification() == null) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private void logUtteranceProcessed(
            Long userId,
            SubmitUtteranceContext context,
            AiNextQuestionResponse aiResponse,
            boolean heartDeducted,
            int remainingHearts,
            SessionStatus sessionStatus
    ) {
        log.info(
                "세션 발화를 처리했습니다. userId={} sessionId={} turnId={} turnClassification={} filledSlotCount={} heartDeducted={} remainingHearts={} sessionStatus={}",
                userId,
                context.sessionId(),
                context.currentTurnId(),
                aiResponse.turnClassification(),
                aiResponse.filledSlots().size(),
                heartDeducted,
                remainingHearts,
                sessionStatus);
    }

    private boolean shouldDeductHeart(int remainingHearts, AiNextQuestionResponse response) {
        return response.turnClassification() == TurnClassification.INVALID_RESPONSE
                && remainingHearts > 0;
    }

    private Set<String> newlyFilledSlotNames(List<SubmitSlotStatus> slotStatuses, List<AiFilledSlot> filledSlots) {
        Set<String> allowedSlotNames = slotStatuses.stream()
                .map(SubmitSlotStatus::slotName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return filledSlots.stream()
                .map(AiFilledSlot::slotName)
                .filter(allowedSlotNames::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean allFulfilled(List<SubmitSlotStatus> slotStatuses, Set<String> newlyFilledSlotNames) {
        return slotStatuses.stream()
                .allMatch(slot -> slot.fulfilled() || newlyFilledSlotNames.contains(slot.slotName()));
    }

    private String validNextQuestionTargetSlotName(
            String nextQuestionTargetSlotName,
            List<SubmitSlotStatus> slotStatuses,
            Set<String> newlyFilledSlotNames
    ) {
        if (nextQuestionTargetSlotName == null || nextQuestionTargetSlotName.isBlank()) {
            return null;
        }
        String normalized = nextQuestionTargetSlotName.trim();
        return slotStatuses.stream()
                .filter(slot -> slot.slotName().equals(normalized))
                .filter(slot -> !slot.fulfilled())
                .filter(slot -> !newlyFilledSlotNames.contains(slot.slotName()))
                .map(SubmitSlotStatus::slotName)
                .findFirst()
                .orElse(null);
    }

    private AiSlotEvidencePolicy parseEvidencePolicy(String evidencePolicy) {
        if (evidencePolicy == null || evidencePolicy.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(evidencePolicy, AiSlotEvidencePolicy.class);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private UserUtteranceResponse completedResponse(
            Long sessionId,
            int remainingHearts,
            boolean heartDeducted,
            TurnClassification turnClassification
    ) {
        return new UserUtteranceResponse(
                sessionId,
                null,
                null,
                remainingHearts,
                true,
                heartDeducted,
                turnClassification);
    }

    private SubmitUtteranceContext loadSubmitUtteranceContext(Long userId, Long sessionId) {
        List<SubmitUtteranceContextRow> rows = sessionRepository.findSubmitUtteranceContextRows(userId, sessionId);
        if (rows.isEmpty()) {
            if (sessionRepository.existsById(sessionId)) {
                throw new ApiException(ErrorCode.FORBIDDEN);
            }
            throw new ApiException(ErrorCode.SESSION_NOT_FOUND);
        }

        SubmitUtteranceContextRow first = rows.getFirst();
        List<SubmitSlotStatus> slotStatuses = rows.stream()
                .filter(row -> row.getSlotName() != null)
                .map(row -> new SubmitSlotStatus(
                        row.getSlotName(),
                        Boolean.TRUE.equals(row.getSlotFulfilled()),
                        row.getSlotDescription(),
                        row.getSlotEvidencePolicy()))
                .toList();

        return new SubmitUtteranceContext(
                first.getSessionId(),
                first.getScenarioId(),
                SessionStatus.valueOf(first.getSessionStatus()),
                first.getRemainingHearts(),
                first.getScenarioTitle(),
                first.getScenarioAiRole(),
                first.getScenarioSituation(),
                first.getScenarioGoal(),
                first.getCurrentTurnId(),
                first.getCurrentTurnSequence(),
                first.getCurrentTurnAiQuestion(),
                first.getCurrentTurnTranslatedQuestion(),
                first.getCurrentTurnTargetSlotName(),
                slotStatuses);
    }

    private AiNextQuestionRequest toAiNextQuestionRequest(SubmitUtteranceContext context, String userUtterance) {
        return new AiNextQuestionRequest(
                context.currentTurnAiQuestion(),
                context.currentTurnTranslatedQuestion(),
                context.currentTurnTargetSlotName(),
                userUtterance,
                context.scenarioTitle(),
                context.scenarioAiRole(),
                context.scenarioSituation(),
                context.scenarioGoal(),
                context.slotStatuses().stream()
                        .map(slot -> new AiNextQuestionSlotStatus(
                                slot.slotName(),
                                slot.description(),
                                slot.fulfilled(),
                                parseEvidencePolicy(slot.evidencePolicy())))
                        .toList());
    }

    private record SubmitUtteranceContext(
            Long sessionId,
            Long scenarioId,
            SessionStatus sessionStatus,
            int remainingHearts,
            String scenarioTitle,
            String scenarioAiRole,
            String scenarioSituation,
            String scenarioGoal,
            Long currentTurnId,
            Integer currentTurnSequence,
            String currentTurnAiQuestion,
            String currentTurnTranslatedQuestion,
            String currentTurnTargetSlotName,
            List<SubmitSlotStatus> slotStatuses
    ) {
    }

    private record SubmitSlotStatus(
            String slotName,
            boolean fulfilled,
            String description,
            String evidencePolicy
    ) {
    }
}

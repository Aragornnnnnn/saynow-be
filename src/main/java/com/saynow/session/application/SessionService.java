// 3차 MVP 프리톡 세션 시작, 발화 제출, 중도 종료를 처리하는 서비스
package com.saynow.session.application;

import com.saynow.auth.domain.User;
import com.saynow.auth.infrastructure.UserRepository;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.common.observability.RequestTraceContext;
import com.saynow.scenario.application.ScenarioService;
import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.ScenarioQuestion;
import com.saynow.scenario.domain.UserScenarioProgress;
import com.saynow.scenario.infrastructure.ScenarioQuestionRepository;
import com.saynow.scenario.infrastructure.ScenarioRepository;
import com.saynow.scenario.infrastructure.UserScenarioProgressRepository;
import com.saynow.session.api.dto.SessionProgressResponse;
import com.saynow.session.api.dto.SessionStartResponse;
import com.saynow.session.api.dto.SessionTurnResponse;
import com.saynow.session.api.dto.SubmittedTurnResponse;
import com.saynow.session.api.dto.UserUtteranceRequest;
import com.saynow.session.api.dto.UserUtteranceResponse;
import com.saynow.session.domain.Session;
import com.saynow.session.domain.SessionStatus;
import com.saynow.session.domain.SessionTurn;
import com.saynow.session.domain.InnerThoughtType;
import com.saynow.session.infrastructure.SessionRepository;
import com.saynow.session.infrastructure.SessionTurnRepository;
import com.saynow.session.infrastructure.ai.AiConversationClient;
import com.saynow.session.infrastructure.ai.AiFixedQuestion;
import com.saynow.session.infrastructure.ai.AiNextQuestionScenarioContext;
import com.saynow.session.infrastructure.ai.AiNextQuestionRequest;
import com.saynow.session.infrastructure.ai.AiNextQuestionResponse;
import com.saynow.session.infrastructure.ai.AiScenarioContext;
import com.saynow.session.infrastructure.ai.AiTurnContext;
import com.saynow.session.infrastructure.ai.AiTurnFeedbackRequest;
import com.saynow.session.infrastructure.ai.AiTurnFeedbackStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private static final String OPENING_INNER_THOUGHT = "첫 질문으로 대화를 시작했으니 편하게 답해주면 좋겠다.";
    private static final InnerThoughtType OPENING_INNER_THOUGHT_TYPE = InnerThoughtType.NORMAL;

    private final ScenarioRepository scenarioRepository;
    private final ScenarioQuestionRepository scenarioQuestionRepository;
    private final SessionRepository sessionRepository;
    private final SessionTurnRepository turnRepository;
    private final UserScenarioProgressRepository userScenarioProgressRepository;
    private final UserRepository userRepository;
    private final AiConversationClient aiConversationClient;
    private final PlatformTransactionManager transactionManager;

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
        ScenarioQuestion firstQuestion = scenarioQuestionRepository.findByScenarioAndSequence(scenario, 1)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_SERVER_ERROR));
        Session session = sessionRepository.save(new Session(user, scenario));
        SessionTurn firstTurn = turnRepository.save(new SessionTurn(
                session,
                firstQuestion,
                firstQuestion.getSequence(),
                firstQuestion.getQuestionEn(),
                firstQuestion.getQuestionKo(),
                OPENING_INNER_THOUGHT,
                OPENING_INNER_THOUGHT_TYPE));
        logStageLatency("session_start", "save_initial_turn", userId, session.getId(), stageStartedAt);

        log.info("세션을 시작했습니다. userId={} scenarioId={} sessionId={}", userId, scenarioId, session.getId());
        return new SessionStartResponse(
                session.getId(),
                scenario.getId(),
                scenario.getTotalQuestionCount(),
                toTurnResponse(firstTurn),
                new SessionProgressResponse(1, scenario.getTotalQuestionCount(), false));
    }

    public UserUtteranceResponse submitUtterance(Long userId, Long sessionId, UserUtteranceRequest request) {
        validateUserUtterance(request);
        String userUtterance = request.userUtterance().trim();

        long stageStartedAt = System.nanoTime();
        SubmitUtteranceContext context = executeReadOnly(() -> loadSubmitUtteranceContext(userId, sessionId));
        logStageLatency("submit_utterance", "load_context", userId, sessionId, stageStartedAt);

        stageStartedAt = System.nanoTime();
        AiNextQuestionResponse nextQuestionResponse = null;
        if (context.nextQuestion() != null) {
            nextQuestionResponse = aiConversationClient.generateNextQuestion(new AiNextQuestionRequest(
                    context.sessionId(),
                    context.currentTurnId(),
                    context.currentSequence(),
                    toNextQuestionScenarioContext(context),
                    new AiTurnContext(context.currentAiQuestion(), context.currentTranslatedQuestion(), userUtterance),
                    toFixedQuestion(context.nextQuestion())));
            validateNextQuestionResponse(nextQuestionResponse);
        }
        logStageLatency("submit_utterance", "generate_next_question", userId, sessionId, stageStartedAt);

        stageStartedAt = System.nanoTime();
        AiTurnFeedbackStatusResponse turnFeedback = aiConversationClient.generateTurnFeedback(new AiTurnFeedbackRequest(
                context.sessionId(),
                context.currentTurnId(),
                context.currentSequence(),
                toScenarioContext(context),
                new AiTurnContext(context.currentAiQuestion(), context.currentTranslatedQuestion(), userUtterance)));
        validateTurnFeedbackResponse(turnFeedback, context.sessionId(), context.currentTurnId());
        logStageLatency("submit_utterance", "request_turn_feedback", userId, sessionId, stageStartedAt);

        stageStartedAt = System.nanoTime();
        AiNextQuestionResponse generatedNextQuestion = nextQuestionResponse;
        UserUtteranceResponse response = executeInTransaction(() -> persistSubmittedUtterance(
                userId,
                sessionId,
                context,
                userUtterance,
                generatedNextQuestion,
                turnFeedback));
        logStageLatency("submit_utterance", "persist_result_transaction", userId, sessionId, stageStartedAt);

        log.info(
                "세션 발화를 처리했습니다. userId={} sessionId={} turnId={} submittedSequence={} turnFeedbackStatus={} progressCompleted={}",
                userId,
                sessionId,
                context.currentTurnId(),
                context.currentSequence(),
                turnFeedback.feedbackStatus(),
                response.progress().completed());
        return response;
    }

    private SubmitUtteranceContext loadSubmitUtteranceContext(Long userId, Long sessionId) {
        Session session = findOwnedSession(userId, sessionId);
        assertInProgress(session);
        SessionTurn currentTurn = turnRepository.findFirstBySessionAndUserUtteranceIsNullOrderBySequenceAsc(session)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_ALREADY_COMPLETED));
        ScenarioQuestion nextQuestion = scenarioQuestionRepository
                .findByScenarioAndSequence(session.getScenario(), currentTurn.getSequence() + 1)
                .orElse(null);
        Scenario scenario = session.getScenario();
        return new SubmitUtteranceContext(
                session.getId(),
                scenario.getId(),
                scenario.getTitle(),
                scenario.getBriefing(),
                scenario.getConversationGoal(),
                scenario.getCounterpartRole(),
                scenario.getTotalQuestionCount(),
                currentTurn.getId(),
                currentTurn.getSequence(),
                currentTurn.getAiQuestion(),
                currentTurn.getTranslatedQuestion(),
                nextQuestion == null ? null : toFixedQuestionContext(nextQuestion));
    }

    private UserUtteranceResponse persistSubmittedUtterance(
            Long userId,
            Long sessionId,
            SubmitUtteranceContext context,
            String userUtterance,
            AiNextQuestionResponse nextQuestionResponse,
            AiTurnFeedbackStatusResponse turnFeedback
    ) {
        int updatedRows = turnRepository.updateUserUtteranceIfPending(
                context.currentTurnId(),
                sessionId,
                userId,
                SessionStatus.IN_PROGRESS,
                userUtterance,
                nextQuestionResponse == null ? null : nextQuestionResponse.innerThought(),
                nextQuestionResponse == null ? null : nextQuestionResponse.innerThoughtType());
        if (updatedRows == 0) {
            throw new ApiException(ErrorCode.SESSION_ALREADY_COMPLETED);
        }
        SessionTurn nextTurn = null;
        if (context.nextQuestion() != null) {
            Session session = sessionRepository.getReferenceById(sessionId);
            ScenarioQuestion nextQuestion = scenarioQuestionRepository.getReferenceById(context.nextQuestion().id());
            nextTurn = turnRepository.save(new SessionTurn(
                    session,
                    nextQuestion,
                    context.nextQuestion().sequence(),
                    nextQuestionResponse.aiQuestion(),
                    nextQuestionResponse.translatedQuestion()));
        }

        boolean completed = nextTurn == null;
        int currentSequence = completed ? context.currentSequence() : nextTurn.getSequence();
        return new UserUtteranceResponse(
                new SubmittedTurnResponse(
                        context.currentTurnId(),
                        context.currentSequence(),
                        turnFeedback.feedbackStatus(),
                        nextQuestionResponse == null ? null : nextQuestionResponse.innerThought(),
                        nextQuestionResponse == null ? null : nextQuestionResponse.innerThoughtType()),
                nextTurn == null ? null : toTurnResponse(nextTurn),
                new SessionProgressResponse(currentSequence, context.totalQuestionCount(), completed));
    }

    @Transactional
    public void abandonSession(Long userId, Long sessionId) {
        Session session = findOwnedSession(userId, sessionId);
        assertInProgress(session);
        session.abandon(LocalDateTime.now());
        log.info("세션을 중도 종료했습니다. userId={} sessionId={}", userId, sessionId);
    }

    private User findUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_REQUIRED));
    }

    private Session findOwnedSession(Long userId, Long sessionId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> sessionRepository.existsById(sessionId)
                        ? new ApiException(ErrorCode.FORBIDDEN)
                        : new ApiException(ErrorCode.SESSION_NOT_FOUND));
    }

    private void assertInProgress(Session session) {
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new ApiException(ErrorCode.SESSION_ALREADY_COMPLETED);
        }
    }

    private void validateUserUtterance(UserUtteranceRequest request) {
        if (request == null || request.userUtterance() == null || request.userUtterance().isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void assertPlayable(User user, Scenario scenario) {
        if (scenario.getCategory().isLocked()) {
            throw new ApiException(ErrorCode.CATEGORY_LOCKED, scenario.getCategory().getLockReason());
        }
        if (scenario.isLocked()) {
            throw new ApiException(ErrorCode.SCENARIO_LOCKED, scenario.getLockReason());
        }

        List<Scenario> scenarios = scenarioRepository.findByCategoryOrderByDisplayOrderAsc(scenario.getCategory());
        Map<Long, Boolean> completedByScenarioId = userScenarioProgressRepository.findByUserIdAndScenarioIdIn(
                        user.getId(),
                        scenarios.stream().map(Scenario::getId).toList())
                .stream()
                .collect(Collectors.toMap(progress -> progress.getScenario().getId(), UserScenarioProgress::isCompleted));

        boolean previousCompleted = true;
        for (Scenario candidate : scenarios) {
            if (candidate.getId().equals(scenario.getId())) {
                if (!previousCompleted) {
                    throw new ApiException(ErrorCode.SCENARIO_LOCKED, ScenarioService.PREVIOUS_SCENARIO_NOT_COMPLETED);
                }
                return;
            }
            previousCompleted = completedByScenarioId.getOrDefault(candidate.getId(), false);
        }
    }

    private void validateNextQuestionResponse(AiNextQuestionResponse response) {
        if (response == null
                || response.aiQuestion() == null
                || response.aiQuestion().isBlank()
                || response.translatedQuestion() == null
                || response.translatedQuestion().isBlank()
                || response.innerThought() == null
                || response.innerThought().isBlank()
                || response.innerThoughtType() == null) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private void validateTurnFeedbackResponse(
            AiTurnFeedbackStatusResponse response,
            Long sessionId,
            Long turnId
    ) {
        if (response == null
                || response.sessionId() == null
                || response.turnId() == null
                || response.feedbackStatus() == null
                || !response.sessionId().equals(sessionId)
                || !response.turnId().equals(turnId)) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private AiScenarioContext toScenarioContext(Scenario scenario) {
        return new AiScenarioContext(
                scenario.getId(),
                scenario.getTitle(),
                scenario.getBriefing(),
                scenario.getConversationGoal(),
                scenario.getCounterpartRole());
    }

    private AiScenarioContext toScenarioContext(SubmitUtteranceContext context) {
        return new AiScenarioContext(
                context.scenarioId(),
                context.scenarioTitle(),
                context.scenarioBriefing(),
                context.scenarioConversationGoal(),
                context.scenarioCounterpartRole());
    }

    private AiNextQuestionScenarioContext toNextQuestionScenarioContext(SubmitUtteranceContext context) {
        return new AiNextQuestionScenarioContext(
                context.scenarioId(),
                context.scenarioTitle(),
                context.scenarioBriefing(),
                context.scenarioConversationGoal(),
                context.scenarioCounterpartRole());
    }

    private AiFixedQuestion toFixedQuestion(ScenarioQuestion question) {
        return new AiFixedQuestion(
                question.getId(),
                question.getSequence(),
                question.getQuestionEn(),
                question.getQuestionKo());
    }

    private AiFixedQuestion toFixedQuestion(FixedQuestionContext question) {
        return new AiFixedQuestion(
                question.id(),
                question.sequence(),
                question.questionEn(),
                question.questionKo());
    }

    private FixedQuestionContext toFixedQuestionContext(ScenarioQuestion question) {
        return new FixedQuestionContext(
                question.getId(),
                question.getSequence(),
                question.getQuestionEn(),
                question.getQuestionKo());
    }

    private SessionTurnResponse toTurnResponse(SessionTurn turn) {
        return new SessionTurnResponse(
                turn.getId(),
                turn.getSequence(),
                turn.getAiQuestion(),
                turn.getTranslatedQuestion());
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

    private <T> T executeReadOnly(Supplier<T> supplier) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
        definition.setReadOnly(true);
        return new TransactionTemplate(transactionManager, definition).execute(status -> supplier.get());
    }

    private <T> T executeInTransaction(Supplier<T> supplier) {
        return new TransactionTemplate(transactionManager).execute(status -> supplier.get());
    }

    private record SubmitUtteranceContext(
            Long sessionId,
            Long scenarioId,
            String scenarioTitle,
            String scenarioBriefing,
            String scenarioConversationGoal,
            String scenarioCounterpartRole,
            int totalQuestionCount,
            Long currentTurnId,
            int currentSequence,
            String currentAiQuestion,
            String currentTranslatedQuestion,
            FixedQuestionContext nextQuestion
    ) {
    }

    private record FixedQuestionContext(
            Long id,
            int sequence,
            String questionEn,
            String questionKo
    ) {
    }
}

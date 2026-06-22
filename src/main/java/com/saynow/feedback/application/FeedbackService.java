// 3차 MVP 완료 가능한 프리톡 세션의 최종 피드백을 생성하고 조회 응답으로 조립하는 서비스
package com.saynow.feedback.application;

import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.common.observability.RequestTraceContext;
import com.saynow.feedback.api.dto.FeedbackResponse;
import com.saynow.feedback.api.dto.TurnFeedbackResponse;
import com.saynow.feedback.domain.FeedbackStatus;
import com.saynow.feedback.domain.SessionFeedback;
import com.saynow.feedback.domain.TurnFeedback;
import com.saynow.feedback.infrastructure.SessionFeedbackRepository;
import com.saynow.feedback.infrastructure.TurnFeedbackRepository;
import com.saynow.scenario.domain.UserScenarioProgress;
import com.saynow.scenario.infrastructure.UserScenarioProgressRepository;
import com.saynow.session.domain.Session;
import com.saynow.session.domain.SessionStatus;
import com.saynow.session.domain.SessionTurn;
import com.saynow.session.infrastructure.SessionRepository;
import com.saynow.session.infrastructure.SessionTurnRepository;
import com.saynow.session.infrastructure.ai.AiConversationClient;
import com.saynow.session.infrastructure.ai.AiScenarioContext;
import com.saynow.session.infrastructure.ai.AiSessionFeedbackRequest;
import com.saynow.session.infrastructure.ai.AiSessionFeedbackResponse;
import com.saynow.session.infrastructure.ai.AiSessionTurnFeedbackResponse;
import com.saynow.session.infrastructure.ai.FeedbackType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final SessionRepository sessionRepository;
    private final SessionTurnRepository turnRepository;
    private final SessionFeedbackRepository sessionFeedbackRepository;
    private final TurnFeedbackRepository turnFeedbackRepository;
    private final UserScenarioProgressRepository userScenarioProgressRepository;
    private final AiConversationClient aiConversationClient;
    private final PlatformTransactionManager transactionManager;

    public FeedbackResponse createFeedback(Long userId, Long sessionId) {
        long stageStartedAt = System.nanoTime();
        FeedbackLoadResult loadResult = executeReadOnly(() -> loadFeedbackContext(userId, sessionId));
        logStageLatency("feedback", "load_context", userId, sessionId, stageStartedAt);
        if (loadResult.existingResponse() != null) {
            return loadResult.existingResponse();
        }

        stageStartedAt = System.nanoTime();
        AiSessionFeedbackResponse aiFeedback = aiConversationClient.generateSessionFeedback(new AiSessionFeedbackRequest(
                loadResult.context().sessionId(),
                new AiScenarioContext(
                        loadResult.context().scenarioId(),
                        loadResult.context().scenarioTitle(),
                        loadResult.context().scenarioBriefing(),
                        loadResult.context().scenarioConversationGoal(),
                        loadResult.context().scenarioCounterpartRole()),
                loadResult.context().turnIds()));
        validateAiFeedback(aiFeedback, sessionId, loadResult.context().turnIds());
        logStageLatency("feedback", "generate_session_feedback", userId, sessionId, stageStartedAt);

        stageStartedAt = System.nanoTime();
        FeedbackResponse response = executeInTransaction(() -> persistFeedback(userId, sessionId, aiFeedback));
        logStageLatency("feedback", "save_session_feedback", userId, sessionId, stageStartedAt);

        log.info(
                "세션 피드백 생성을 완료했습니다. userId={} sessionId={} nativeScore={} turnCount={}",
                userId,
                sessionId,
                response.nativeScore(),
                response.turnFeedbacks().size());
        return response;
    }

    private FeedbackLoadResult loadFeedbackContext(Long userId, Long sessionId) {
        Session session = findOwnedSession(userId, sessionId);
        List<SessionTurn> turns = turnRepository.findBySessionOrderBySequenceAsc(session);
        SessionFeedback existingFeedback = sessionFeedbackRepository.findBySession(session).orElse(null);
        if (existingFeedback != null) {
            return new FeedbackLoadResult(
                    toResponse(
                            session,
                            existingFeedback,
                            turns,
                            turnFeedbackRepository.findBySessionFeedbackWithTurnOrderByTurnSequenceAsc(existingFeedback)),
                    null);
        }

        validateFeedbackReady(session, turns);
        return new FeedbackLoadResult(null, toFeedbackContext(session, turns));
    }

    private FeedbackResponse persistFeedback(Long userId, Long sessionId, AiSessionFeedbackResponse aiFeedback) {
        Session session = findOwnedSession(userId, sessionId);
        List<SessionTurn> turns = turnRepository.findBySessionOrderBySequenceAsc(session);
        SessionFeedback existingFeedback = sessionFeedbackRepository.findBySession(session).orElse(null);
        if (existingFeedback != null) {
            return toResponse(
                    session,
                    existingFeedback,
                    turns,
                    turnFeedbackRepository.findBySessionFeedbackWithTurnOrderByTurnSequenceAsc(existingFeedback));
        }
        validateFeedbackReady(session, turns);

        LocalDateTime now = LocalDateTime.now();
        SessionFeedback sessionFeedback = sessionFeedbackRepository.save(new SessionFeedback(
                session,
                FeedbackStatus.READY,
                aiFeedback.nativeScore(),
                aiFeedback.highlightMessage(),
                now));
        Map<Long, AiSessionTurnFeedbackResponse> aiFeedbackByTurnId = aiFeedback.turnFeedbacks().stream()
                .collect(Collectors.toMap(AiSessionTurnFeedbackResponse::turnId, Function.identity()));
        List<TurnFeedback> turnFeedbacks = turns.stream()
                .map(turn -> toTurnFeedback(sessionFeedback, turn, aiFeedbackByTurnId.get(turn.getId()), now))
                .toList();
        turnFeedbackRepository.saveAll(turnFeedbacks);
        session.complete(now);
        userScenarioProgressRepository.findByUserAndScenario(session.getUser(), session.getScenario())
                .orElseGet(() -> userScenarioProgressRepository.save(new UserScenarioProgress(session.getUser(), session.getScenario())))
                .markCompleted(now);
        return toResponse(session, sessionFeedback, turns, turnFeedbacks);
    }

    private Session findOwnedSession(Long userId, Long sessionId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> sessionRepository.existsById(sessionId)
                        ? new ApiException(ErrorCode.FORBIDDEN)
                        : new ApiException(ErrorCode.SESSION_NOT_FOUND));
    }

    private void validateFeedbackReady(Session session, List<SessionTurn> turns) {
        if (session.getStatus() == SessionStatus.ABANDONED) {
            throw new ApiException(ErrorCode.SESSION_ALREADY_COMPLETED);
        }
        long answeredCount = turns.stream().filter(SessionTurn::isAnswered).count();
        if (answeredCount != session.getScenario().getTotalQuestionCount() || turns.size() != session.getScenario().getTotalQuestionCount()) {
            throw new ApiException(ErrorCode.SESSION_NOT_COMPLETED);
        }
    }

    private void validateAiFeedback(AiSessionFeedbackResponse feedback, Long sessionId, List<Long> turnIds) {
        if (feedback == null
                || feedback.sessionId() == null
                || !feedback.sessionId().equals(sessionId)
                || feedback.nativeScore() < 0
                || feedback.nativeScore() > 100
                || feedback.highlightMessage() == null
                || feedback.highlightMessage().isBlank()
                || feedback.turnFeedbacks() == null) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
        Map<Long, AiSessionTurnFeedbackResponse> feedbackByTurnId = feedback.turnFeedbacks().stream()
                .filter(turnFeedback -> turnFeedback.turnId() != null)
                .collect(Collectors.toMap(AiSessionTurnFeedbackResponse::turnId, Function.identity(), (left, right) -> left));
        boolean allTurnsHaveFeedback = turnIds.stream().allMatch(feedbackByTurnId::containsKey);
        boolean allFeedbacksAreValid = feedback.turnFeedbacks().stream().allMatch(turnFeedback ->
                turnFeedback.turnId() != null
                        && turnFeedback.feedbackType() != null
                        && turnFeedback.koreanAnalogy() != null
                        && !turnFeedback.koreanAnalogy().isBlank()
                        && isValidTurnFeedbackPayload(turnFeedback));
        if (!allTurnsHaveFeedback || feedbackByTurnId.size() != turnIds.size() || feedback.turnFeedbacks().size() != turnIds.size() || !allFeedbacksAreValid) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private boolean isValidTurnFeedbackPayload(AiSessionTurnFeedbackResponse feedback) {
        if (feedback.feedbackType() == FeedbackType.GOOD) {
            return isBlank(feedback.positiveFeedback())
                    && !isBlank(feedback.feedbackDetail())
                    && isBlank(feedback.correctionExpression())
                    && isBlank(feedback.correctionReason());
        }
        return !isBlank(feedback.positiveFeedback())
                && isBlank(feedback.feedbackDetail())
                && !isBlank(feedback.correctionExpression())
                && !isBlank(feedback.correctionReason())
                && isBlank(feedback.benchmarkMessage());
    }

    private TurnFeedback toTurnFeedback(
            SessionFeedback sessionFeedback,
            SessionTurn turn,
            AiSessionTurnFeedbackResponse feedback,
            LocalDateTime generatedAt
    ) {
        return new TurnFeedback(
                sessionFeedback,
                turn,
                FeedbackStatus.READY,
                feedback.feedbackType(),
                feedback.koreanAnalogy(),
                feedback.positiveFeedback(),
                feedback.feedbackDetail(),
                feedback.correctionExpression(),
                feedback.correctionReason(),
                feedback.benchmarkMessage(),
                generatedAt);
    }

    private FeedbackResponse toResponse(
            Session session,
            SessionFeedback sessionFeedback,
            List<SessionTurn> turns,
            List<TurnFeedback> turnFeedbacks
    ) {
        Map<Long, TurnFeedback> feedbackByTurnId = turnFeedbacks.stream()
                .collect(Collectors.toMap(feedback -> feedback.getTurn().getId(), Function.identity()));
        return new FeedbackResponse(
                session.getId(),
                sessionFeedback.getNativeScore(),
                sessionFeedback.getHighlightMessage(),
                turns.stream()
                        .map(turn -> toTurnFeedbackResponse(turn, feedbackByTurnId.get(turn.getId())))
                        .toList());
    }

    private TurnFeedbackResponse toTurnFeedbackResponse(SessionTurn turn, TurnFeedback feedback) {
        return new TurnFeedbackResponse(
                turn.getId(),
                turn.getSequence(),
                turn.getAiQuestion(),
                turn.getTranslatedQuestion(),
                turn.getUserUtterance(),
                feedback.getFeedbackType(),
                feedback.getKoreanAnalogy(),
                feedback.getPositiveFeedback(),
                feedback.getFeedbackDetail(),
                feedback.getCorrectionExpression(),
                feedback.getCorrectionReason(),
                feedback.getBenchmarkMessage());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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

    private FeedbackContext toFeedbackContext(Session session, List<SessionTurn> turns) {
        return new FeedbackContext(
                session.getId(),
                session.getScenario().getId(),
                session.getScenario().getTitle(),
                session.getScenario().getBriefing(),
                session.getScenario().getConversationGoal(),
                session.getScenario().getCounterpartRole(),
                turns.stream().map(SessionTurn::getId).toList());
    }

    private <T> T executeReadOnly(Supplier<T> supplier) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
        definition.setReadOnly(true);
        return new TransactionTemplate(transactionManager, definition).execute(status -> supplier.get());
    }

    private <T> T executeInTransaction(Supplier<T> supplier) {
        return new TransactionTemplate(transactionManager).execute(status -> supplier.get());
    }

    private record FeedbackLoadResult(
            FeedbackResponse existingResponse,
            FeedbackContext context
    ) {
    }

    private record FeedbackContext(
            Long sessionId,
            Long scenarioId,
            String scenarioTitle,
            String scenarioBriefing,
            String scenarioConversationGoal,
            String scenarioCounterpartRole,
            List<Long> turnIds
    ) {
    }
}

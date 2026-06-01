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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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

    @Transactional
    public FeedbackResponse createFeedback(Long userId, Long sessionId) {
        long stageStartedAt = System.nanoTime();
        Session session = findOwnedSession(userId, sessionId);
        List<SessionTurn> turns = turnRepository.findBySessionOrderBySequenceAsc(session);
        logStageLatency("feedback", "load_context", userId, sessionId, stageStartedAt);

        SessionFeedback existingFeedback = sessionFeedbackRepository.findBySession(session).orElse(null);
        if (existingFeedback != null) {
            return toResponse(session, existingFeedback, turns, turnFeedbackRepository.findBySessionFeedbackOrderByTurnSequenceAsc(existingFeedback));
        }

        validateFeedbackReady(session, turns);

        stageStartedAt = System.nanoTime();
        AiSessionFeedbackResponse aiFeedback = aiConversationClient.generateSessionFeedback(new AiSessionFeedbackRequest(
                session.getId(),
                new AiScenarioContext(
                        session.getScenario().getId(),
                        session.getScenario().getTitle(),
                        session.getScenario().getBriefing(),
                        session.getScenario().getConversationGoal()),
                turns.stream().map(SessionTurn::getId).toList()));
        validateAiFeedback(aiFeedback, sessionId, turns);
        logStageLatency("feedback", "generate_session_feedback", userId, sessionId, stageStartedAt);

        stageStartedAt = System.nanoTime();
        LocalDateTime now = LocalDateTime.now();
        SessionFeedback sessionFeedback = sessionFeedbackRepository.save(new SessionFeedback(
                session,
                FeedbackStatus.READY,
                aiFeedback.nativeScore(),
                aiFeedback.nativeLevelLabel(),
                aiFeedback.summary(),
                now));
        Map<Long, AiSessionTurnFeedbackResponse> aiFeedbackByTurnId = aiFeedback.turnFeedbacks().stream()
                .collect(Collectors.toMap(AiSessionTurnFeedbackResponse::turnId, Function.identity()));
        List<TurnFeedback> turnFeedbacks = turns.stream()
                .map(turn -> toTurnFeedback(sessionFeedback, turn, aiFeedbackByTurnId.get(turn.getId()), now))
                .map(turnFeedbackRepository::save)
                .toList();
        session.complete(now);
        userScenarioProgressRepository.findByUserAndScenario(session.getUser(), session.getScenario())
                .orElseGet(() -> userScenarioProgressRepository.save(new UserScenarioProgress(session.getUser(), session.getScenario())))
                .markCompleted(now);
        logStageLatency("feedback", "save_session_feedback", userId, sessionId, stageStartedAt);

        log.info(
                "세션 피드백 생성을 완료했습니다. userId={} sessionId={} nativeScore={} turnCount={}",
                userId,
                sessionId,
                sessionFeedback.getNativeScore(),
                turnFeedbacks.size());
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

    private void validateAiFeedback(AiSessionFeedbackResponse feedback, Long sessionId, List<SessionTurn> turns) {
        if (feedback == null
                || feedback.sessionId() == null
                || !feedback.sessionId().equals(sessionId)
                || feedback.nativeScore() < 0
                || feedback.nativeScore() > 100
                || feedback.nativeLevelLabel() == null
                || feedback.nativeLevelLabel().isBlank()
                || feedback.summary() == null
                || feedback.summary().isBlank()
                || feedback.turnFeedbacks() == null) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
        Map<Long, AiSessionTurnFeedbackResponse> feedbackByTurnId = feedback.turnFeedbacks().stream()
                .collect(Collectors.toMap(AiSessionTurnFeedbackResponse::turnId, Function.identity()));
        boolean allTurnsHaveFeedback = turns.stream().allMatch(turn -> feedbackByTurnId.containsKey(turn.getId()));
        if (!allTurnsHaveFeedback || feedbackByTurnId.size() != turns.size()) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
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
                feedback.correctionPoint(),
                feedback.correctionReason(),
                feedback.plusOneExpression(),
                feedback.praiseSummary(),
                feedback.praiseReason(),
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
                sessionFeedback.getNativeLevelLabel(),
                sessionFeedback.getSummary(),
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
                feedback.getCorrectionPoint(),
                feedback.getCorrectionReason(),
                feedback.getPlusOneExpression(),
                feedback.getPraiseSummary(),
                feedback.getPraiseReason());
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
}

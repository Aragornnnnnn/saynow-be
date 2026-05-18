// 완료 가능한 세션의 최종 피드백을 생성하고 조회 응답으로 조립하는 서비스
package com.saynow.feedback.application;

import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.feedback.api.dto.FeedbackResponse;
import com.saynow.feedback.api.dto.TurnFeedbackResponse;
import com.saynow.feedback.domain.SessionFeedback;
import com.saynow.feedback.domain.TurnFeedback;
import com.saynow.feedback.infrastructure.SessionFeedbackRepository;
import com.saynow.feedback.infrastructure.TurnFeedbackRepository;
import com.saynow.session.domain.Session;
import com.saynow.session.domain.SessionTurn;
import com.saynow.session.domain.SessionStatus;
import com.saynow.session.infrastructure.SessionRepository;
import com.saynow.session.infrastructure.SessionTurnRepository;
import com.saynow.session.infrastructure.ai.AiConversationClient;
import com.saynow.session.infrastructure.ai.AiFeedbackRequest;
import com.saynow.session.infrastructure.ai.AiFeedbackResponse;
import com.saynow.session.infrastructure.ai.AiFeedbackTurnRequest;
import com.saynow.session.infrastructure.ai.AiTurnFeedbackResponse;
import com.saynow.scenario.domain.UserScenarioClear;
import com.saynow.scenario.infrastructure.UserScenarioClearRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final int MAX_FEEDBACK_RETRY = 3;

    private final SessionRepository sessionRepository;
    private final SessionTurnRepository turnRepository;
    private final SessionFeedbackRepository sessionFeedbackRepository;
    private final TurnFeedbackRepository turnFeedbackRepository;
    private final UserScenarioClearRepository userScenarioClearRepository;
    private final AiConversationClient aiConversationClient;

    @Transactional
    public FeedbackResponse createFeedback(Long userId, Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
        if (!session.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.SESSION_ACCESS_DENIED);
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

        AiFeedbackResponse aiFeedback = generateFeedbackWithRetry(session, turns);
        validateAiFeedback(aiFeedback, turns);

        SessionFeedback sessionFeedback = sessionFeedbackRepository.save(new SessionFeedback(
                session,
                session.getStatus() == SessionStatus.SUCCESS,
                aiFeedback.comprehensionScore(),
                aiFeedback.feedbackSummary()));
        Map<Long, AiTurnFeedbackResponse> aiFeedbackByTurnId = aiFeedback.turnFeedbacks().stream()
                .collect(Collectors.toMap(AiTurnFeedbackResponse::turnId, Function.identity()));
        List<TurnFeedback> savedTurnFeedbacks = turns.stream()
                .map(turn -> saveTurnFeedback(sessionFeedback, turn, aiFeedbackByTurnId.get(turn.getId())))
                .toList();

        if (session.getStatus() == SessionStatus.SUCCESS) {
            UserScenarioClear clear = userScenarioClearRepository.findByUserAndScenario(session.getUser(), session.getScenario())
                    .orElseGet(() -> userScenarioClearRepository.save(new UserScenarioClear(session.getUser(), session.getScenario())));
            clear.markCleared();
        }

        return toResponse(session, sessionFeedback, savedTurnFeedbacks);
    }

    private AiFeedbackResponse generateFeedbackWithRetry(Session session, List<SessionTurn> turns) {
        AiFeedbackRequest request = new AiFeedbackRequest(
                session.getScenario().getTitle(),
                session.getScenario().getGoal(),
                turns.stream()
                        .map(turn -> new AiFeedbackTurnRequest(turn.getId(), turn.getAiQuestion(), turn.getUserUtterance()))
                        .toList());

        ApiException lastException = null;
        for (int attempt = 0; attempt < MAX_FEEDBACK_RETRY; attempt++) {
            try {
                return aiConversationClient.generateFeedback(request);
            } catch (ApiException exception) {
                lastException = exception;
            }
        }
        throw new ApiException(ErrorCode.FEEDBACK_GENERATION_FAILED, lastException == null
                ? ErrorCode.FEEDBACK_GENERATION_FAILED.getMessage()
                : lastException.getMessage());
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
}

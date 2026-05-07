package com.saynow.feedback.application;

import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.feedback.api.dto.FeedbackResponse;
import com.saynow.feedback.api.dto.TurnFeedbackResponse;
import com.saynow.feedback.domain.FeedbackStatus;
import com.saynow.feedback.domain.SessionFeedback;
import com.saynow.feedback.infrastructure.SessionFeedbackRepository;
import com.saynow.feedback.infrastructure.TurnFeedbackRepository;
import com.saynow.practice.domain.PracticeSession;
import com.saynow.practice.domain.PracticeTurn;
import com.saynow.practice.domain.SessionStatus;
import com.saynow.practice.infrastructure.PracticeSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FeedbackService {

    private final PracticeSessionRepository sessionRepository;
    private final SessionFeedbackRepository sessionFeedbackRepository;
    private final TurnFeedbackRepository turnFeedbackRepository;

    public FeedbackService(
            PracticeSessionRepository sessionRepository,
            SessionFeedbackRepository sessionFeedbackRepository,
            TurnFeedbackRepository turnFeedbackRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.sessionFeedbackRepository = sessionFeedbackRepository;
        this.turnFeedbackRepository = turnFeedbackRepository;
    }

    public FeedbackResponse getFeedback(String sessionId) {
        PracticeSession session = sessionRepository.findByPublicId(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));

        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            throw new ApiException(ErrorCode.SESSION_IN_PROGRESS);
        }

        SessionFeedback sessionFeedback = sessionFeedbackRepository.findBySession(session)
                .orElseThrow(() -> new ApiException(ErrorCode.FEEDBACK_NOT_FOUND));

        if (sessionFeedback.getFeedbackStatus() == FeedbackStatus.GENERATING) {
            throw new ApiException(ErrorCode.FEEDBACK_GENERATING);
        }
        if (sessionFeedback.getFeedbackStatus() == FeedbackStatus.FAILED) {
            throw new ApiException(ErrorCode.FEEDBACK_FAILED);
        }

        return new FeedbackResponse(
                session.getPublicId(),
                sessionFeedback.getScenarioResult(),
                sessionFeedback.getTotalUnderstoodScore(),
                sessionFeedback.getSummary(),
                sessionFeedback.getAverageScoreDelta(),
                turnFeedbackRepository.findBySessionFeedbackOrderByTurnTurnIndexAsc(sessionFeedback).stream()
                        .map(turnFeedback -> {
                            PracticeTurn turn = turnFeedback.getTurn();
                            return new TurnFeedbackResponse(
                                    turn.getId(),
                                    turn.getTurnIndex(),
                                    turn.getPrompt().getPromptText(),
                                    turn.getUserTranscript(),
                                    turn.getSpeechStartedAfterMs(),
                                    toSeconds(turn.getSpeechStartedAfterMs()),
                                    turnFeedback.getUnderstoodScore(),
                                    turnFeedback.getHeardAs(),
                                    turnFeedback.getBetterExpression(),
                                    turnFeedback.getScoreDelta(),
                                    turnFeedback.getImprovedUnderstoodScore(),
                                    turnFeedback.getReason());
                        })
                        .toList());
    }

    private Double toSeconds(Integer milliseconds) {
        if (milliseconds == null) {
            return null;
        }
        return milliseconds / 1000.0;
    }
}

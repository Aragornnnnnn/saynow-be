package com.saynow.feedback.service;

import com.saynow.common.exception.ApiException;
import com.saynow.feedback.controller.dto.FeedbackResponse;
import com.saynow.feedback.controller.dto.TurnFeedbackResponse;
import com.saynow.feedback.domain.FeedbackStatus;
import com.saynow.feedback.domain.SessionFeedback;
import com.saynow.feedback.repository.SessionFeedbackRepository;
import com.saynow.feedback.repository.TurnFeedbackRepository;
import com.saynow.practice.domain.PracticeSession;
import com.saynow.practice.domain.PracticeTurn;
import com.saynow.practice.domain.SessionStatus;
import com.saynow.practice.repository.PracticeSessionRepository;
import org.springframework.http.HttpStatus;
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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "세션을 찾을 수 없습니다."));

        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SESSION_IN_PROGRESS", "아직 진행 중인 세션입니다.");
        }

        SessionFeedback sessionFeedback = sessionFeedbackRepository.findBySession(session)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FEEDBACK_NOT_FOUND", "피드백을 찾을 수 없습니다."));

        if (sessionFeedback.getFeedbackStatus() == FeedbackStatus.GENERATING) {
            throw new ApiException(HttpStatus.ACCEPTED, "FEEDBACK_GENERATING", "피드백을 생성 중입니다.");
        }
        if (sessionFeedback.getFeedbackStatus() == FeedbackStatus.FAILED) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FEEDBACK_FAILED", "피드백 생성에 실패했습니다.");
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

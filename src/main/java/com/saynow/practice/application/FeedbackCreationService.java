package com.saynow.practice.application;

import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.feedback.domain.SessionFeedback;
import com.saynow.feedback.domain.TurnFeedback;
import com.saynow.feedback.infrastructure.SessionFeedbackRepository;
import com.saynow.feedback.infrastructure.TurnFeedbackRepository;
import com.saynow.practice.domain.PracticeSession;
import com.saynow.practice.domain.PracticeTurn;
import com.saynow.practice.infrastructure.PracticeTurnRepository;
import com.saynow.practice.infrastructure.ai.AiPracticeClient;
import com.saynow.practice.infrastructure.ai.AiSessionFeedbackRequest;
import com.saynow.practice.infrastructure.ai.AiSessionFeedbackResult;
import com.saynow.practice.infrastructure.ai.AiTurnFeedbackResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackCreationService {

    private final SessionFeedbackRepository sessionFeedbackRepository;
    private final TurnFeedbackRepository turnFeedbackRepository;
    private final PracticeTurnRepository turnRepository;
    private final AiPracticeClient aiPracticeClient;

    public void createReadyFeedback(PracticeSession session) {
        if (sessionFeedbackRepository.existsBySession(session)) {
            return;
        }

        List<PracticeTurn> turns = turnRepository.findBySessionOrderByTurnIndexAsc(session);
        AiSessionFeedbackResult aiFeedback = aiPracticeClient.createSessionFeedback(new AiSessionFeedbackRequest(
                session.getPublicId(),
                session.getScenario(),
                session.getStatus(),
                session.getFilledSlots(),
                turns));
        validateFeedback(aiFeedback, turns);

        SessionFeedback sessionFeedback = sessionFeedbackRepository.save(new SessionFeedback(
                session,
                session.getStatus(),
                aiFeedback.totalUnderstoodScore(),
                aiFeedback.summary()));

        for (int i = 0; i < turns.size(); i++) {
            PracticeTurn turn = turns.get(i);
            AiTurnFeedbackResult turnFeedback = aiFeedback.turnFeedback().get(i);
            turnFeedbackRepository.save(new TurnFeedback(
                    sessionFeedback,
                    turn,
                    turnFeedback.understoodScore(),
                    turnFeedback.heardAs(),
                    turnFeedback.betterExpression(),
                    turnFeedback.scoreDelta(),
                    turnFeedback.improvedUnderstoodScore(),
                    turnFeedback.reason()));
        }
    }

    private void validateFeedback(AiSessionFeedbackResult feedback, List<PracticeTurn> turns) {
        if (feedback == null
                || feedback.summary() == null
                || feedback.summary().isBlank()
                || feedback.turnFeedback() == null
                || feedback.turnFeedback().size() != turns.size()) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID, "AI 서버 세션 피드백 응답이 올바르지 않습니다.");
        }
    }
}

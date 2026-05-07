package com.saynow.practice.application;

import com.saynow.feedback.domain.FeedbackStatus;
import com.saynow.feedback.domain.SessionFeedback;
import com.saynow.feedback.domain.TurnFeedback;
import com.saynow.feedback.repository.SessionFeedbackRepository;
import com.saynow.feedback.repository.TurnFeedbackRepository;
import com.saynow.practice.domain.PracticeSession;
import com.saynow.practice.domain.PracticeTurn;
import com.saynow.practice.domain.SessionStatus;
import com.saynow.practice.infrastructure.PracticeTurnRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeedbackCreationService {

    private final SessionFeedbackRepository sessionFeedbackRepository;
    private final TurnFeedbackRepository turnFeedbackRepository;
    private final PracticeTurnRepository turnRepository;

    public FeedbackCreationService(
            SessionFeedbackRepository sessionFeedbackRepository,
            TurnFeedbackRepository turnFeedbackRepository,
            PracticeTurnRepository turnRepository
    ) {
        this.sessionFeedbackRepository = sessionFeedbackRepository;
        this.turnFeedbackRepository = turnFeedbackRepository;
        this.turnRepository = turnRepository;
    }

    public void createReadyFeedback(PracticeSession session, LocalDateTime now) {
        if (sessionFeedbackRepository.existsBySession(session)) {
            return;
        }

        boolean success = session.getStatus() == SessionStatus.SUCCESS;
        SessionFeedback sessionFeedback = sessionFeedbackRepository.save(new SessionFeedback(
                session,
                session.getStatus(),
                success ? 85 : 60,
                success
                        ? "대화 전체를 보면 필요한 정보가 전달되어 시나리오를 완료했어요."
                        : "대화 전체를 보면 필요한 정보가 충분히 전달되지 않아 시나리오를 완료하지 못했어요.",
                success ? 8 : 12,
                FeedbackStatus.READY,
                now,
                now));

        List<PracticeTurn> turns = turnRepository.findBySessionOrderByTurnIndexAsc(session);
        for (PracticeTurn turn : turns) {
            int understoodScore = success ? Math.min(95, 80 + turn.getTurnIndex() * 2) : Math.max(45, 65 - turn.getTurnIndex() * 3);
            int scoreDelta = success ? 8 : 12;
            turnFeedbackRepository.save(new TurnFeedback(
                    sessionFeedback,
                    turn,
                    understoodScore,
                    "외국인은 발화의 핵심 의도를 이해할 가능성이 높아요.",
                    turn.getTurnIndex() == 1 ? "I'd like an iced americano, please." : null,
                    scoreDelta,
                    Math.min(100, understoodScore + scoreDelta),
                    turn.getTurnIndex() == 1
                            ? "주문 상황에서는 'I'd like'를 쓰면 더 자연스럽게 들려요."
                            : "이미 충분히 자연스럽게 답했어요.",
                    now));
        }
    }
}

package com.saynow.feedback.domain;

import com.saynow.common.domain.BaseTimeEntity;
import com.saynow.session.domain.SessionTurn;
import com.saynow.session.infrastructure.ai.FeedbackType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "turn_feedbacks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TurnFeedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_feedback_id", nullable = false)
    private SessionFeedback sessionFeedback;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turn_id", nullable = false)
    @Getter
    private SessionTurn turn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Getter
    private FeedbackStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false, length = 30)
    @Getter
    private FeedbackType feedbackType;

    @Column(name = "korean_analogy", nullable = false, columnDefinition = "text")
    @Getter
    private String koreanAnalogy;

    @Column(name = "positive_feedback", columnDefinition = "text")
    @Getter
    private String positiveFeedback;

    @Column(name = "feedback_detail", nullable = false, columnDefinition = "text")
    @Getter
    private String feedbackDetail;

    @Column(name = "benchmark_message", columnDefinition = "text")
    @Getter
    private String benchmarkMessage;

    @Column(name = "generated_at", nullable = false)
    @Getter
    private LocalDateTime generatedAt;

    public TurnFeedback(
            SessionFeedback sessionFeedback,
            SessionTurn turn,
            FeedbackStatus status,
            FeedbackType feedbackType,
            String koreanAnalogy,
            String positiveFeedback,
            String feedbackDetail,
            String benchmarkMessage,
            LocalDateTime generatedAt
    ) {
        this.sessionFeedback = sessionFeedback;
        this.turn = turn;
        this.status = status;
        this.feedbackType = feedbackType;
        this.koreanAnalogy = koreanAnalogy;
        this.positiveFeedback = positiveFeedback;
        this.feedbackDetail = feedbackDetail;
        this.benchmarkMessage = benchmarkMessage;
        this.generatedAt = generatedAt;
    }

}

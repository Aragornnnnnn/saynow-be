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

    @Column(name = "correction_point", columnDefinition = "text")
    @Getter
    private String correctionPoint;

    @Column(name = "correction_reason", columnDefinition = "text")
    @Getter
    private String correctionReason;

    @Column(name = "plus_one_expression", length = 500)
    @Getter
    private String plusOneExpression;

    @Column(name = "praise_summary", columnDefinition = "text")
    @Getter
    private String praiseSummary;

    @Column(name = "praise_reason", columnDefinition = "text")
    @Getter
    private String praiseReason;

    @Column(name = "generated_at", nullable = false)
    @Getter
    private LocalDateTime generatedAt;

    public TurnFeedback(
            SessionFeedback sessionFeedback,
            SessionTurn turn,
            FeedbackStatus status,
            FeedbackType feedbackType,
            String koreanAnalogy,
            String correctionPoint,
            String correctionReason,
            String plusOneExpression,
            String praiseSummary,
            String praiseReason,
            LocalDateTime generatedAt
    ) {
        this.sessionFeedback = sessionFeedback;
        this.turn = turn;
        this.status = status;
        this.feedbackType = feedbackType;
        this.koreanAnalogy = koreanAnalogy;
        this.correctionPoint = correctionPoint;
        this.correctionReason = correctionReason;
        this.plusOneExpression = plusOneExpression;
        this.praiseSummary = praiseSummary;
        this.praiseReason = praiseReason;
        this.generatedAt = generatedAt;
    }

}

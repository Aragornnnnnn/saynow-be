package com.saynow.feedback.domain;

import com.saynow.common.domain.BaseTimeEntity;
import com.saynow.session.domain.SessionTurn;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "feedback_required", nullable = false)
    @Getter
    private boolean feedbackRequired;

    @Column(name = "native_understanding", columnDefinition = "text")
    @Getter
    private String nativeUnderstanding;

    @Column(name = "native_language_interpretation", columnDefinition = "text")
    @Getter
    private String nativeLanguageInterpretation;

    @Column(name = "better_expression", length = 500)
    @Getter
    private String betterExpression;

    public TurnFeedback(
            SessionFeedback sessionFeedback,
            SessionTurn turn,
            boolean feedbackRequired,
            String nativeUnderstanding,
            String nativeLanguageInterpretation,
            String betterExpression
    ) {
        this.sessionFeedback = sessionFeedback;
        this.turn = turn;
        this.feedbackRequired = feedbackRequired;
        this.nativeUnderstanding = nativeUnderstanding;
        this.nativeLanguageInterpretation = nativeLanguageInterpretation;
        this.betterExpression = betterExpression;
    }

}

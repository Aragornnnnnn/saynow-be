package com.saynow.feedback.domain;

import com.saynow.common.domain.BaseTimeEntity;
import com.saynow.practice.domain.PracticeTurn;
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

@Entity
@Table(name = "turn_feedbacks")
public class TurnFeedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_feedback_id", nullable = false)
    private SessionFeedback sessionFeedback;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turn_id", nullable = false)
    private PracticeTurn turn;

    @Column(name = "understood_score", nullable = false)
    private int understoodScore;

    @Column(name = "heard_as", nullable = false, columnDefinition = "text")
    private String heardAs;

    @Column(name = "better_expression", length = 500)
    private String betterExpression;

    @Column(name = "score_delta", nullable = false)
    private int scoreDelta;

    @Column(name = "improved_understood_score", nullable = false)
    private int improvedUnderstoodScore;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    protected TurnFeedback() {
    }

    public TurnFeedback(
            SessionFeedback sessionFeedback,
            PracticeTurn turn,
            int understoodScore,
            String heardAs,
            String betterExpression,
            int scoreDelta,
            int improvedUnderstoodScore,
            String reason
    ) {
        this.sessionFeedback = sessionFeedback;
        this.turn = turn;
        this.understoodScore = understoodScore;
        this.heardAs = heardAs;
        this.betterExpression = betterExpression;
        this.scoreDelta = scoreDelta;
        this.improvedUnderstoodScore = improvedUnderstoodScore;
        this.reason = reason;
    }

    public PracticeTurn getTurn() {
        return turn;
    }

    public int getUnderstoodScore() {
        return understoodScore;
    }

    public String getHeardAs() {
        return heardAs;
    }

    public String getBetterExpression() {
        return betterExpression;
    }

    public int getScoreDelta() {
        return scoreDelta;
    }

    public int getImprovedUnderstoodScore() {
        return improvedUnderstoodScore;
    }

    public String getReason() {
        return reason;
    }
}

package com.saynow.feedback.domain;

import com.saynow.practice.domain.PracticeSession;
import com.saynow.practice.domain.SessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_feedbacks")
public class SessionFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private PracticeSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario_result", nullable = false, length = 20)
    private SessionStatus scenarioResult;

    @Column(name = "total_understood_score", nullable = false)
    private int totalUnderstoodScore;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "average_score_delta", nullable = false)
    private int averageScoreDelta;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_status", nullable = false, length = 20)
    private FeedbackStatus feedbackStatus;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SessionFeedback() {
    }

    public SessionFeedback(
            PracticeSession session,
            SessionStatus scenarioResult,
            int totalUnderstoodScore,
            String summary,
            int averageScoreDelta,
            FeedbackStatus feedbackStatus,
            LocalDateTime generatedAt,
            LocalDateTime now
    ) {
        this.session = session;
        this.scenarioResult = scenarioResult;
        this.totalUnderstoodScore = totalUnderstoodScore;
        this.summary = summary;
        this.averageScoreDelta = averageScoreDelta;
        this.feedbackStatus = feedbackStatus;
        this.generatedAt = generatedAt;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public PracticeSession getSession() {
        return session;
    }

    public SessionStatus getScenarioResult() {
        return scenarioResult;
    }

    public int getTotalUnderstoodScore() {
        return totalUnderstoodScore;
    }

    public String getSummary() {
        return summary;
    }

    public int getAverageScoreDelta() {
        return averageScoreDelta;
    }

    public FeedbackStatus getFeedbackStatus() {
        return feedbackStatus;
    }
}

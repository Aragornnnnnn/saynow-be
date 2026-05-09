package com.saynow.feedback.domain;

import com.saynow.common.domain.BaseTimeEntity;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "session_feedbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionFeedback extends BaseTimeEntity {

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

    public SessionFeedback(
            PracticeSession session,
            SessionStatus scenarioResult,
            int totalUnderstoodScore,
            String summary
    ) {
        if (scenarioResult != SessionStatus.SUCCESS && scenarioResult != SessionStatus.FAILURE) {
            throw new IllegalArgumentException("scenarioResult must be SUCCESS or FAILURE");
        }
        this.session = session;
        this.scenarioResult = scenarioResult;
        this.totalUnderstoodScore = totalUnderstoodScore;
        this.summary = summary;
    }

}

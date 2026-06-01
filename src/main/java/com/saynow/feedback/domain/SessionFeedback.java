package com.saynow.feedback.domain;

import com.saynow.common.domain.BaseTimeEntity;
import com.saynow.session.domain.Session;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    private Session session;

    @Column(nullable = false)
    private boolean cleared;

    @Column(name = "comprehension_score", nullable = false)
    private int comprehensionScore;

    @Column(name = "feedback_summary", nullable = false, columnDefinition = "text")
    private String feedbackSummary;

    public SessionFeedback(
            Session session,
            boolean cleared,
            int comprehensionScore,
            String feedbackSummary
    ) {
        this.session = session;
        this.cleared = cleared;
        this.comprehensionScore = comprehensionScore;
        this.feedbackSummary = feedbackSummary;
    }

}

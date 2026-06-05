package com.saynow.feedback.domain;

import com.saynow.common.domain.BaseTimeEntity;
import com.saynow.session.domain.Session;
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

import java.time.LocalDateTime;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackStatus status;

    @Column(name = "native_score", nullable = false)
    private int nativeScore;

    @Column(name = "highlight_message", nullable = false, columnDefinition = "text")
    private String highlightMessage;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    public SessionFeedback(
            Session session,
            FeedbackStatus status,
            int nativeScore,
            String highlightMessage,
            LocalDateTime generatedAt
    ) {
        this.session = session;
        this.status = status;
        this.nativeScore = nativeScore;
        this.highlightMessage = highlightMessage;
        this.generatedAt = generatedAt;
    }

}

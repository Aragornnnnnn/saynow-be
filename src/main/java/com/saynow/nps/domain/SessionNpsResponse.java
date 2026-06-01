// 사용자가 특정 세션에 제출한 NPS 평가를 저장하는 엔티티
package com.saynow.nps.domain;

import com.saynow.auth.domain.User;
import com.saynow.common.domain.BaseTimeEntity;
import com.saynow.session.domain.Session;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "session_nps_responses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_session_nps_responses_user_session",
                columnNames = {"user_id", "session_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionNpsResponse extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(nullable = false)
    private int score;

    @Column(name = "low_score_reason", columnDefinition = "text")
    private String lowScoreReason;

    public SessionNpsResponse(
            User user,
            Session session,
            int score,
            String lowScoreReason
    ) {
        this.user = user;
        this.session = session;
        this.score = score;
        this.lowScoreReason = lowScoreReason;
    }
}

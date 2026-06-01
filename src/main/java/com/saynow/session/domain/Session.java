// 사용자의 시나리오 진행 세션을 저장하는 엔티티
package com.saynow.session.domain;

import com.saynow.auth.domain.User;
import com.saynow.common.domain.BaseTimeEntity;
import com.saynow.scenario.domain.Scenario;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Session extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "remaining_hearts", nullable = false)
    private int remainingHearts;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Session(User user, Scenario scenario) {
        this.user = user;
        this.scenario = scenario;
        this.status = SessionStatus.IN_PROGRESS;
        this.remainingHearts = scenario.getHeart();
    }

    public void complete(SessionStatus status, LocalDateTime completedAt) {
        if (status != SessionStatus.SUCCESS && status != SessionStatus.FAILURE) {
            throw new IllegalArgumentException("status must be SUCCESS or FAILURE");
        }
        this.status = status;
        this.completedAt = completedAt;
    }

    public void abandon(LocalDateTime completedAt) {
        this.status = SessionStatus.ABANDONED;
        this.completedAt = completedAt;
    }

    public boolean isInProgress() {
        return status == SessionStatus.IN_PROGRESS;
    }

    public boolean isOwnedBy(Long userId) {
        return user != null && user.getId().equals(userId);
    }

    public void decreaseHeart() {
        if (remainingHearts > 0) {
            remainingHearts--;
        }
    }
}

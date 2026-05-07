package com.saynow.practice.domain;

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
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
@Table(name = "practice_sessions")
public class PracticeSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36, columnDefinition = "char(36)")
    private String publicId;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "max_follow_up_count", nullable = false)
    private int maxFollowUpCount;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "exit_reason", length = 100)
    private String exitReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected PracticeSession() {
    }

    public PracticeSession(String publicId, Scenario scenario, LocalDateTime now) {
        this.publicId = publicId;
        this.scenario = scenario;
        this.status = SessionStatus.IN_PROGRESS;
        this.maxFollowUpCount = scenario.getMaxFollowUpCount();
        this.startedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void finish(SessionStatus status, LocalDateTime endedAt, String exitReason) {
        this.status = status;
        this.endedAt = endedAt;
        this.exitReason = exitReason;
        this.updatedAt = endedAt;
    }

    public boolean isInProgress() {
        return status == SessionStatus.IN_PROGRESS;
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public int getMaxFollowUpCount() {
        return maxFollowUpCount;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }
}

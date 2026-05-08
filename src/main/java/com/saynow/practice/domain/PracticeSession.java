package com.saynow.practice.domain;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "practice_sessions")
public class PracticeSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36, columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "current_babsae_text", nullable = false, length = 500)
    private String currentBabsaeText;

    @Column(name = "current_babsae_tts_url", length = 500)
    private String currentBabsaeTtsUrl;

    @Column(name = "follow_up_count", nullable = false)
    private int followUpCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filled_slots", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> filledSlots = new LinkedHashMap<>();

    @Column(name = "mic_ready_latency_ms")
    private Integer micReadyLatencyMs;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    protected PracticeSession() {
    }

    public PracticeSession(String publicId, Scenario scenario, LocalDateTime now) {
        this.publicId = publicId;
        this.scenario = scenario;
        this.status = SessionStatus.IN_PROGRESS;
        this.currentBabsaeText = scenario.getOpeningBabsaeText();
        this.currentBabsaeTtsUrl = scenario.getOpeningTtsUrl();
        this.followUpCount = 0;
        this.filledSlots = new LinkedHashMap<>();
        this.startedAt = now;
    }

    public void recordMicReady(Integer micReadyLatencyMs) {
        this.micReadyLatencyMs = micReadyLatencyMs;
    }

    public void applyFollowUp(String questionText, String ttsUrl) {
        this.currentBabsaeText = questionText;
        this.currentBabsaeTtsUrl = ttsUrl;
        this.followUpCount++;
    }

    public void finish(SessionStatus status, String messageText, String ttsUrl, LocalDateTime endedAt) {
        this.status = status;
        this.endedAt = endedAt;
        this.currentBabsaeText = messageText;
        this.currentBabsaeTtsUrl = ttsUrl;
    }

    public void abandon(LocalDateTime endedAt) {
        this.status = SessionStatus.ABANDONED;
        this.endedAt = endedAt;
    }

    public void putFilledSlot(String slotKey, String slotValue) {
        ensureFilledSlots();
        this.filledSlots.put(slotKey, slotValue);
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

    public String getCurrentBabsaeText() {
        return currentBabsaeText;
    }

    public String getCurrentBabsaeTtsUrl() {
        return currentBabsaeTtsUrl;
    }

    public int getFollowUpCount() {
        return followUpCount;
    }

    public Map<String, String> getFilledSlots() {
        ensureFilledSlots();
        return filledSlots;
    }

    public Integer getMicReadyLatencyMs() {
        return micReadyLatencyMs;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    private void ensureFilledSlots() {
        if (filledSlots == null) {
            filledSlots = new LinkedHashMap<>();
        }
    }
}

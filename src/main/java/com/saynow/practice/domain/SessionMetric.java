package com.saynow.practice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_metrics")
public class SessionMetric {

    public static final String MIC_READY_LATENCY_MS = "MIC_READY_LATENCY_MS";
    public static final String UNIT_MS = "MS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private PracticeSession session;

    @Column(name = "metric_key", nullable = false, length = 80)
    private String metricKey;

    @Column(name = "metric_value", nullable = false)
    private long metricValue;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SessionMetric() {
    }

    public SessionMetric(PracticeSession session, String metricKey, long metricValue, String unit, LocalDateTime now) {
        this.session = session;
        this.metricKey = metricKey;
        this.metricValue = metricValue;
        this.unit = unit;
        this.recordedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(long metricValue, LocalDateTime now) {
        this.metricValue = metricValue;
        this.recordedAt = now;
        this.updatedAt = now;
    }

    public long getMetricValue() {
        return metricValue;
    }
}

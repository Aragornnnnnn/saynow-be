package com.saynow.practice.domain;

import com.saynow.scenario.domain.ScenarioSlot;
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
@Table(name = "session_slot_values")
public class SessionSlotValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private PracticeSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_slot_id", nullable = false)
    private ScenarioSlot scenarioSlot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_turn_id", nullable = false)
    private PracticeTurn sourceTurn;

    @Column(name = "slot_value", nullable = false, length = 255)
    private String slotValue;

    @Column(name = "filled_at", nullable = false)
    private LocalDateTime filledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected SessionSlotValue() {
    }

    public SessionSlotValue(PracticeSession session, ScenarioSlot scenarioSlot, PracticeTurn sourceTurn, String slotValue, LocalDateTime now) {
        this.session = session;
        this.scenarioSlot = scenarioSlot;
        this.sourceTurn = sourceTurn;
        this.slotValue = slotValue;
        this.filledAt = now;
        this.createdAt = now;
    }

    public ScenarioSlot getScenarioSlot() {
        return scenarioSlot;
    }

    public String getSlotValue() {
        return slotValue;
    }
}

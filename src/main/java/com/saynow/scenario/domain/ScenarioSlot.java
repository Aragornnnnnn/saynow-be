package com.saynow.scenario.domain;

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
@Table(name = "scenario_slots")
public class ScenarioSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(name = "slot_key", nullable = false, length = 80)
    private String slotKey;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "slot_order", nullable = false)
    private int slotOrder;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ScenarioSlot() {
    }

    public Long getId() {
        return id;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public String getSlotKey() {
        return slotKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSlotOrder() {
        return slotOrder;
    }

    public boolean isRequired() {
        return required;
    }
}

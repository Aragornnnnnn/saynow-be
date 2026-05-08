package com.saynow.scenario.domain;

import com.saynow.common.domain.BaseTimeEntity;
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

@Entity
@Table(name = "scenarios")
public class Scenario extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ScenarioCategory category;

    @Column(name = "scenario_key", nullable = false, unique = true, length = 80)
    private String scenarioKey;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScenarioDifficulty difficulty;

    @Column(name = "situation_description", nullable = false, columnDefinition = "text")
    private String situationDescription;

    @Column(name = "success_goal", nullable = false, length = 255)
    private String successGoal;

    @Column(name = "opening_babsae_text", nullable = false, length = 500)
    private String openingBabsaeText;

    @Column(name = "opening_tts_url", length = 500)
    private String openingTtsUrl;

    @Column(name = "max_follow_up_count", nullable = false)
    private int maxFollowUpCount;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    protected Scenario() {
    }

    public Long getId() {
        return id;
    }

    public ScenarioCategory getCategory() {
        return category;
    }

    public String getScenarioKey() {
        return scenarioKey;
    }

    public String getTitle() {
        return title;
    }

    public ScenarioDifficulty getDifficulty() {
        return difficulty;
    }

    public String getSituationDescription() {
        return situationDescription;
    }

    public String getSuccessGoal() {
        return successGoal;
    }

    public String getOpeningBabsaeText() {
        return openingBabsaeText;
    }

    public String getOpeningTtsUrl() {
        return openingTtsUrl;
    }

    public int getMaxFollowUpCount() {
        return maxFollowUpCount;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

}

package com.saynow.scenario.domain;

import com.saynow.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scenario_categories")
public class ScenarioCategory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_key", nullable = false, unique = true, length = 50)
    private String categoryKey;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    protected ScenarioCategory() {
    }

    public Long getId() {
        return id;
    }

    public String getCategoryKey() {
        return categoryKey;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

}

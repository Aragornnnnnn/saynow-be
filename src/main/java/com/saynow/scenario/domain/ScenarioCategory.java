package com.saynow.scenario.domain;

import com.saynow.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scenario_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

}

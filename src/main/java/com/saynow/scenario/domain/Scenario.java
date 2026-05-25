package com.saynow.scenario.domain;

import com.saynow.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "scenarios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Scenario extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "original_question", nullable = false, length = 500)
    private String originalQuestion;

    @Column(name = "translated_question", nullable = false, length = 500)
    private String translatedQuestion;

    @Column(nullable = false, length = 255)
    private String goal;

    @Column(nullable = false, length = 255)
    private String situation;

    @Column(length = 20)
    private String emoji;

    @Column(name = "background_image", nullable = false, length = 500)
    private String backgroundImage;

    @Column(nullable = false)
    private int heart;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

}

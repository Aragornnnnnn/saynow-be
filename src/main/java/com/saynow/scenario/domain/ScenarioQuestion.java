// 시나리오별 고정 프리톡 질문을 순서대로 저장하는 엔티티
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
@Table(name = "scenario_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScenarioQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(nullable = false)
    private int sequence;

    @Column(name = "question_en", nullable = false, length = 500)
    private String questionEn;

    @Column(name = "question_ko", nullable = false, length = 500)
    private String questionKo;
}

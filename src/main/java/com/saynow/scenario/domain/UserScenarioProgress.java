// 사용자별 시나리오 진행 상태를 저장하는 엔티티
package com.saynow.scenario.domain;

import com.saynow.auth.domain.User;
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
@Table(name = "user_scenario_progress")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserScenarioProgress extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(name = "cleared", nullable = false)
    private boolean cleared;

    public UserScenarioProgress(User user, Scenario scenario) {
        this.user = user;
        this.scenario = scenario;
        this.cleared = false;
    }

    public void markCleared() {
        this.cleared = true;
    }
}

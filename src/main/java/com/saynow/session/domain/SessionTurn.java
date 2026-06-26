// 세션 중 AI 질문과 사용자 발화를 턴 단위로 저장하는 엔티티
package com.saynow.session.domain;

import com.saynow.common.domain.BaseTimeEntity;
import com.saynow.scenario.domain.ScenarioQuestion;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_turns")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionTurn extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_question_id", nullable = false)
    @Getter
    private ScenarioQuestion scenarioQuestion;

    @Column(nullable = false)
    @Getter
    private int sequence;

    @Column(name = "ai_question", nullable = false, length = 500)
    @Getter
    private String aiQuestion;

    @Column(name = "translated_question", nullable = false, length = 500)
    @Getter
    private String translatedQuestion;

    @Column(name = "user_utterance", columnDefinition = "text")
    @Getter
    private String userUtterance;

    @Column(name = "inner_thought", columnDefinition = "text")
    @Getter
    private String innerThought;

    @Enumerated(EnumType.STRING)
    @Column(name = "inner_thought_type", length = 20)
    @Getter
    private InnerThoughtType innerThoughtType;

    @Column(name = "answered_at")
    @Getter
    private LocalDateTime answeredAt;

    public SessionTurn(
            Session session,
            ScenarioQuestion scenarioQuestion,
            int sequence,
            String aiQuestion,
            String translatedQuestion
    ) {
        this(session, scenarioQuestion, sequence, aiQuestion, translatedQuestion, null, null);
    }

    public SessionTurn(
            Session session,
            ScenarioQuestion scenarioQuestion,
            int sequence,
            String aiQuestion,
            String translatedQuestion,
            String innerThought,
            InnerThoughtType innerThoughtType
    ) {
        this.session = session;
        this.scenarioQuestion = scenarioQuestion;
        this.sequence = sequence;
        this.aiQuestion = aiQuestion;
        this.translatedQuestion = translatedQuestion;
        this.innerThought = innerThought;
        this.innerThoughtType = innerThoughtType;
    }

    public boolean isAnswered() {
        return userUtterance != null && !userUtterance.isBlank();
    }

}

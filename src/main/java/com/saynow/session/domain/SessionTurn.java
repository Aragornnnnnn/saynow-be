// 세션 중 AI 질문과 사용자 발화를 턴 단위로 저장하는 엔티티
package com.saynow.session.domain;

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

    public SessionTurn(
            Session session,
            int sequence,
            String aiQuestion,
            String translatedQuestion
    ) {
        this.session = session;
        this.sequence = sequence;
        this.aiQuestion = aiQuestion;
        this.translatedQuestion = translatedQuestion;
    }

    public boolean isAnswered() {
        return userUtterance != null && !userUtterance.isBlank();
    }

    public void submitUserUtterance(String userUtterance) {
        this.userUtterance = userUtterance;
    }

}

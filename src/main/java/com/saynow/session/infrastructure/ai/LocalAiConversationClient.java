// 로컬 개발과 테스트에서 사용할 결정적 AI 대체 클라이언트
package com.saynow.session.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "saynow.ai", name = "client-mode", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalAiConversationClient implements AiConversationClient, AiFeedbackStreamClient {

    private final ObjectMapper objectMapper;

    @Override
    public AiNextQuestionResponse generateNextQuestion(AiNextQuestionRequest request) {
        List<AiSlotStatus> slots = request.slots() == null ? List.of() : request.slots();
        List<AiFilledSlot> newlyFilled = new ArrayList<>();
        for (AiSlotStatus slot : slots) {
            if (!slot.filled()) {
                newlyFilled.add(new AiFilledSlot(slot.slotName()));
                break;
            }
        }

        long remainingAfterFill = slots.stream()
                .filter(slot -> !slot.filled())
                .count() - newlyFilled.size();
        if (remainingAfterFill <= 0) {
            return new AiNextQuestionResponse(null, null, newlyFilled, TurnClassification.ANSWER);
        }

        String nextSlot = slots.stream()
                .filter(slot -> !slot.filled())
                .map(AiSlotStatus::slotName)
                .filter(slotName -> newlyFilled.stream().noneMatch(filled -> filled.slotName().equals(slotName)))
                .findFirst()
                .orElse("detail");
        return new AiNextQuestionResponse(
                "Could you tell me your " + nextSlot + "?",
                nextSlot + "에 대해 말해주시겠어요?",
                newlyFilled,
                TurnClassification.ANSWER);
    }

    @Override
    public AiFeedbackResponse generateFeedback(AiFeedbackRequest request) {
        List<AiTurnFeedbackResponse> turnFeedbacks = request.turns().stream()
                .map(turn -> new AiTurnFeedbackResponse(
                        turn.turnId(),
                        true,
                        "의도는 전달됐지만 표현이 조금 짧게 들립니다.",
                        "한국어로 치면 필요한 것만 짧게 말한 느낌입니다.",
                        "I'd like " + turn.userUtterance() + ", please."))
                .toList();
        return new AiFeedbackResponse(
                82,
                "전체적으로 의도는 전달됐고, 조금 더 자연스러운 표현을 연습하면 좋습니다.",
                turnFeedbacks);
    }

    @Override
    public Flux<AiFeedbackStreamEvent> streamFeedback(AiFeedbackRequest request) {
        AiFeedbackResponse feedback = generateFeedback(request);
        return Flux.concat(
                Flux.just(new AiFeedbackStreamEvent(
                        "summary",
                        json(objectMap(
                                "comprehensionScore", feedback.comprehensionScore(),
                                "feedbackSummary", feedback.feedbackSummary()
                        )))),
                Flux.fromIterable(feedback.turnFeedbacks())
                        .map(turnFeedback -> new AiFeedbackStreamEvent(
                                "turnFeedback",
                                json(objectMap(
                                        "turnId", turnFeedback.turnId(),
                                        "feedbackRequired", turnFeedback.feedbackRequired(),
                                        "nativeUnderstanding", turnFeedback.nativeUnderstanding(),
                                        "nativeLanguageInterpretation", turnFeedback.nativeLanguageInterpretation(),
                                        "betterExpression", turnFeedback.betterExpression()
                                )))),
                Flux.just(new AiFeedbackStreamEvent(
                        "done",
                        json(objectMap("turnCount", feedback.turnFeedbacks().size())))));
    }

    private Map<String, Object> objectMap(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            map.put((String) keyValues[index], keyValues[index + 1]);
        }
        return map;
    }

    private String json(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new AiFeedbackStreamException("로컬 AI 피드백 스트림을 생성할 수 없습니다.", exception);
        }
    }
}

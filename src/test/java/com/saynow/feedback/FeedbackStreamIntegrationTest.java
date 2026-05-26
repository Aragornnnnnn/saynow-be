// AI 피드백 SSE 중계 API의 이벤트 순서와 저장 정책을 검증한다.
package com.saynow.feedback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.IntegrationTestSupport;
import com.saynow.feedback.domain.SessionFeedback;
import com.saynow.feedback.infrastructure.SessionFeedbackRepository;
import com.saynow.feedback.infrastructure.TurnFeedbackRepository;
import com.saynow.session.domain.Session;
import com.saynow.session.infrastructure.SessionRepository;
import com.saynow.session.infrastructure.ai.AiConversationClient;
import com.saynow.session.infrastructure.ai.AiFeedbackRequest;
import com.saynow.session.infrastructure.ai.AiFeedbackResponse;
import com.saynow.session.infrastructure.ai.AiFeedbackStreamClient;
import com.saynow.session.infrastructure.ai.AiFeedbackStreamEvent;
import com.saynow.session.infrastructure.ai.AiFeedbackStreamException;
import com.saynow.session.infrastructure.ai.AiFilledSlot;
import com.saynow.session.infrastructure.ai.AiNextQuestionRequest;
import com.saynow.session.infrastructure.ai.AiNextQuestionResponse;
import com.saynow.session.infrastructure.ai.AiSlotStatus;
import com.saynow.session.infrastructure.ai.AiTurnFeedbackResponse;
import com.saynow.session.infrastructure.ai.TurnClassification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeedbackStreamIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAiConversationClient aiConversationClient;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private SessionFeedbackRepository sessionFeedbackRepository;

    @Autowired
    private TurnFeedbackRepository turnFeedbackRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        aiConversationClient.reset();
    }

    @Test
    void streamsSummaryTurnFeedbackAndDoneInOrderThenPersistsFeedback() throws Exception {
        String accessToken = login("stream-normal-sub|stream-normal@example.com|Stream User");
        long sessionId = completeSession(accessToken);

        String body = performFeedbackStream(accessToken, sessionId);

        assertThat(body).containsSubsequence(
                "event: summary",
                "event: turnFeedback",
                "event: turnFeedback",
                "event: done"
        );
        assertThat(body.trim()).endsWith("data: {\"turnCount\":3}");
        aiConversationClient.lastStreamRequest.turns()
                .forEach(turn -> assertThat(body).contains("\"turnId\":" + turn.turnId()));
        assertThat(aiConversationClient.lastStreamSessionResult()).isEqualTo("SUCCESS");
        assertThat(aiConversationClient.lastStreamAiRole())
                .isEqualTo("미국 공항 입국심사관");
        assertThat(aiConversationClient.lastStreamScenarioSituation())
                .isEqualTo("미국 공항에 도착해 입국심사를 받는 상황입니다. 심사관의 질문에 여행 계획을 차분히 설명해야 합니다.");
        assertThat(aiConversationClient.lastStreamSlots())
                .extracting(AiSlotStatus::slotName, AiSlotStatus::description, AiSlotStatus::filled)
                .containsExactly(
                        tuple("visit_purpose", "사용자가 미국 방문 목적을 여행, 출장, 유학 등으로 설명했는지 여부", true),
                        tuple("stay_duration", "사용자가 미국에 머무를 기간이나 출국 예정 시점을 설명했는지 여부", true),
                        tuple("accommodation", "사용자가 머무를 숙소, 호텔, 주소, 지인 집 등 체류 장소를 설명했는지 여부", true)
                );

        Session session = sessionRepository.findById(sessionId).orElseThrow();
        SessionFeedback savedFeedback = sessionFeedbackRepository.findBySession(session).orElseThrow();
        assertThat(savedFeedback.getComprehensionScore()).isEqualTo(82);
        assertThat(savedFeedback.getFeedbackSummary()).contains("의도는 잘 전달됐지만");
        assertThat(turnFeedbackRepository.findBySessionFeedbackOrderByTurnSequenceAsc(savedFeedback)).hasSize(3);
    }

    @Test
    void relaysAiErrorEventAndDoesNotPersistFeedback() throws Exception {
        String accessToken = login("stream-ai-error-sub|stream-ai-error@example.com|Stream Error User");
        long sessionId = completeSession(accessToken);
        aiConversationClient.streamMode = StreamMode.AI_ERROR;

        String body = performFeedbackStream(accessToken, sessionId);

        assertThat(body).contains("event: error");
        assertThat(body).contains("\"code\":\"AI_GENERATION_FAILED\"");
        assertThat(body).doesNotContain("event: done");

        Session session = sessionRepository.findById(sessionId).orElseThrow();
        assertThat(sessionFeedbackRepository.findBySession(session)).isEmpty();
    }

    @Test
    void returnsErrorEventWhenAiStreamConnectionFails() throws Exception {
        String accessToken = login("stream-failure-sub|stream-failure@example.com|Stream Failure User");
        long sessionId = completeSession(accessToken);
        aiConversationClient.streamMode = StreamMode.CONNECTION_FAILURE;

        String body = performFeedbackStream(accessToken, sessionId);

        assertThat(body).contains("event: error");
        assertThat(body).contains("\"sessionId\":" + sessionId);
        assertThat(body).contains("\"code\":\"AI_GENERATION_FAILED\"");
        assertThat(body).contains("피드백 생성에 실패했습니다.");
    }

    @Test
    void synchronousFeedbackApiStillWorks() throws Exception {
        String accessToken = login("stream-sync-sub|stream-sync@example.com|Stream Sync User");
        long sessionId = completeSession(accessToken);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/feedback", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.comprehensionScore").value(82))
                .andExpect(jsonPath("$.data.turnFeedbacks.length()").value(3));
        assertThat(aiConversationClient.lastGenerateFeedbackSessionResult()).isEqualTo("SUCCESS");
        assertThat(aiConversationClient.lastGenerateFeedbackAiRole())
                .isEqualTo("미국 공항 입국심사관");
        assertThat(aiConversationClient.lastGenerateFeedbackScenarioSituation())
                .isEqualTo("미국 공항에 도착해 입국심사를 받는 상황입니다. 심사관의 질문에 여행 계획을 차분히 설명해야 합니다.");
        assertThat(aiConversationClient.lastGenerateFeedbackSlots())
                .extracting(AiSlotStatus::slotName, AiSlotStatus::description, AiSlotStatus::filled)
                .containsExactly(
                        tuple("visit_purpose", "사용자가 미국 방문 목적을 여행, 출장, 유학 등으로 설명했는지 여부", true),
                        tuple("stay_duration", "사용자가 미국에 머무를 기간이나 출국 예정 시점을 설명했는지 여부", true),
                        tuple("accommodation", "사용자가 머무를 숙소, 호텔, 주소, 지인 집 등 체류 장소를 설명했는지 여부", true)
                );
    }

    private String performFeedbackStream(String accessToken, long sessionId) throws Exception {
        MvcResult started = mockMvc.perform(post("/api/v1/sessions/{sessionId}/feedback/stream", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult completed = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event: ")))
                .andReturn();
        return completed.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private long completeSession(String accessToken) throws Exception {
        long sessionId = startSession(accessToken);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I'm here for sightseeing."}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I'll stay for five days."}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I'll stay at the Midtown Hotel."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbackAvailable").value(true));

        return sessionId;
    }

    private long startSession(String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/scenarios/4/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return body.get("data").get("sessionId").asLong();
    }

    private String login(String idToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"%s",
                                  "nonce":"nonce"
                                }
                                """.formatted(idToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return body.get("data").get("accessToken").asText();
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    @TestConfiguration
    static class FeedbackStreamTestConfiguration {

        @Bean
        @Primary
        TestAiConversationClient testAiConversationClient() {
            return new TestAiConversationClient();
        }
    }

    enum StreamMode {
        NORMAL,
        AI_ERROR,
        CONNECTION_FAILURE
    }

    static class TestAiConversationClient implements AiConversationClient, AiFeedbackStreamClient {

        private final ObjectMapper objectMapper = new ObjectMapper();
        private StreamMode streamMode = StreamMode.NORMAL;
        private AiFeedbackRequest lastGenerateFeedbackRequest;
        private AiFeedbackRequest lastStreamRequest;

        void reset() {
            streamMode = StreamMode.NORMAL;
            lastGenerateFeedbackRequest = null;
            lastStreamRequest = null;
        }

        String lastGenerateFeedbackSessionResult() {
            return lastGenerateFeedbackRequest.sessionResult();
        }

        String lastGenerateFeedbackScenarioSituation() {
            return lastGenerateFeedbackRequest.scenarioSituation();
        }

        String lastGenerateFeedbackAiRole() {
            return lastGenerateFeedbackRequest.aiRole();
        }

        List<AiSlotStatus> lastGenerateFeedbackSlots() {
            return lastGenerateFeedbackRequest.slots();
        }

        String lastStreamSessionResult() {
            return lastStreamRequest.sessionResult();
        }

        String lastStreamScenarioSituation() {
            return lastStreamRequest.scenarioSituation();
        }

        String lastStreamAiRole() {
            return lastStreamRequest.aiRole();
        }

        List<AiSlotStatus> lastStreamSlots() {
            return lastStreamRequest.slots();
        }

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
            lastGenerateFeedbackRequest = request;
            List<AiTurnFeedbackResponse> turnFeedbacks = request.turns().stream()
                    .map(turn -> new AiTurnFeedbackResponse(
                            turn.turnId(),
                            true,
                            "외국인은 주문 의도를 이해했지만 표현이 조금 짧다고 이해했어요.",
                            "한국어로 비유하자면, 필요한 말만 짧게 말한 느낌입니다.",
                            "I'd like " + turn.userUtterance() + ", please."))
                    .toList();
            return new AiFeedbackResponse(
                    82,
                    "전체적으로 의도는 잘 전달됐지만 주문 표현이 조금 짧게 들립니다.",
                    turnFeedbacks);
        }

        @Override
        public Flux<AiFeedbackStreamEvent> streamFeedback(AiFeedbackRequest request) {
            lastStreamRequest = request;
            if (streamMode == StreamMode.CONNECTION_FAILURE) {
                return Flux.error(new AiFeedbackStreamException("AI SSE 연결에 실패했습니다."));
            }
            if (streamMode == StreamMode.AI_ERROR) {
                return Flux.just(new AiFeedbackStreamEvent(
                        "error",
                        json(Map.of(
                                "code", "AI_GENERATION_FAILED",
                                "message", "피드백 생성에 실패했습니다."
                        ))));
            }

            return Flux.concat(
                    Flux.just(new AiFeedbackStreamEvent(
                            "summary",
                            json(linkedMap(
                                    "comprehensionScore", 82,
                                    "feedbackSummary", "전체적으로 의도는 잘 전달됐지만 주문 표현이 조금 짧게 들립니다."
                            )))),
                    Flux.fromIterable(request.turns())
                            .map(turn -> new AiFeedbackStreamEvent(
                                    "turnFeedback",
                                    json(linkedMap(
                                            "turnId", turn.turnId(),
                                            "feedbackRequired", true,
                                            "nativeUnderstanding", "외국인은 주문 의도를 이해했지만 표현이 조금 짧다고 이해했어요.",
                                            "nativeLanguageInterpretation", "한국어로 비유하자면, 필요한 말만 짧게 말한 느낌입니다.",
                                            "betterExpression", "I'd like " + turn.userUtterance() + ", please."
                                    )))),
                    Flux.just(new AiFeedbackStreamEvent(
                            "done",
                            json(Map.of("turnCount", request.turns().size())))));
        }

        private Map<String, Object> linkedMap(Object... keyValues) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int index = 0; index < keyValues.length; index += 2) {
                map.put((String) keyValues[index], keyValues[index + 1]);
            }
            return map;
        }

        private String json(Map<String, Object> data) {
            try {
                return objectMapper.writeValueAsString(data);
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}

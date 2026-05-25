// 2차 MVP 시나리오 목록, 세션 진행, 피드백 생성을 통합 검증한다.
package com.saynow.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.IntegrationTestSupport;
import com.saynow.session.infrastructure.ai.AiConversationClient;
import com.saynow.session.infrastructure.ai.AiFeedbackRequest;
import com.saynow.session.infrastructure.ai.AiFeedbackResponse;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScenarioFlowIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAiConversationClient aiConversationClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        aiConversationClient.reset();
    }

    @Test
    void scenarioFlowUnlocksNextScenarioAfterSuccessfulFeedback() throws Exception {
        String accessToken = login("mvp2-sub-1|mvp2@example.com|MVP2 User");

        mockMvc.perform(get("/api/v1/scenarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0].categoryName").value("Cafe"))
                .andExpect(jsonPath("$.data.categories[0].categoryLocked").value(true))
                .andExpect(jsonPath("$.data.categories[0].categoryLockReason").value("COMING_SOON"))
                .andExpect(jsonPath("$.data.categories[0].scenarios.length()").value(0))
                .andExpect(jsonPath("$.data.categories[1].categoryName").value("Airport"))
                .andExpect(jsonPath("$.data.categories[1].categoryLocked").value(false))
                .andExpect(jsonPath("$.data.categories[1].scenarios[0].scenarioSituation").value("미국 공항에 도착해 입국심사를 받는 상황입니다. 심사관의 질문에 여행 계획을 차분히 설명해야 합니다."))
                .andExpect(jsonPath("$.data.categories[1].scenarios[0].locked").value(false))
                .andExpect(jsonPath("$.data.categories[1].scenarios[1].locked").value(true));

        mockMvc.perform(post("/api/v1/scenarios/1/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_LOCKED"));

        mockMvc.perform(post("/api/v1/scenarios/5/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SCENARIO_LOCKED"));

        long sessionId = startSession(accessToken, 4);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I'm here for sightseeing."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.remainingHearts").value(3))
                .andExpect(jsonPath("$.data.feedbackAvailable").value(false))
                .andExpect(jsonPath("$.data.originalQuestion").value("Could you tell me your stay_duration?"));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I'll stay for five days."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.remainingHearts").value(3))
                .andExpect(jsonPath("$.data.feedbackAvailable").value(false))
                .andExpect(jsonPath("$.data.originalQuestion").value("Could you tell me your accommodation?"));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I'll stay at the Midtown Hotel."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.remainingHearts").value(3))
                .andExpect(jsonPath("$.data.feedbackAvailable").value(true))
                .andExpect(jsonPath("$.data.originalQuestion").value(nullValue()))
                .andExpect(jsonPath("$.data.translatedQuestion").value(nullValue()));

        mockMvc.perform(get("/api/v1/scenarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[1].scenarios[0].cleared").value(true))
                .andExpect(jsonPath("$.data.categories[1].scenarios[1].locked").value(false));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/feedback", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.cleared").value(true))
                .andExpect(jsonPath("$.data.comprehensionScore").value(82))
                .andExpect(jsonPath("$.data.remainingHearts").value(3))
                .andExpect(jsonPath("$.data.turnFeedbacks.length()").value(3))
                .andExpect(jsonPath("$.data.turnFeedbacks[0].sequence").value(1))
                .andExpect(jsonPath("$.data.turnFeedbacks[0].userUtterance").value("I'm here for sightseeing."));

        mockMvc.perform(get("/api/v1/scenarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[1].scenarios[0].cleared").value(true))
                .andExpect(jsonPath("$.data.categories[1].scenarios[1].locked").value(false));
    }

    @Test
    void recommendationRequestDoesNotDeductHeartWhenFilledSlotsAreEmpty() throws Exception {
        assertTurnPolicy(
                "mvp2-sub-2|recommend@example.com|Recommend User",
                "Can you recommend a menu?",
                TurnClassification.ASSISTANCE_REQUEST,
                List.of(),
                "The menu includes iced Americano, latte, cappuccino, and tea. What would you like to order?",
                "메뉴에는 아이스 아메리카노, 라떼, 카푸치노, 차가 있어요. 무엇을 주문하시겠어요?",
                3,
                false);
    }

    @Test
    void menuRequestDoesNotDeductHeartWhenFilledSlotsAreEmpty() throws Exception {
        assertTurnPolicy(
                "mvp2-sub-3|menu@example.com|Menu User",
                "Can I see the menu?",
                TurnClassification.ASSISTANCE_REQUEST,
                List.of(),
                "The menu includes iced Americano, latte, cappuccino, and tea. What would you like to order?",
                "메뉴에는 아이스 아메리카노, 라떼, 카푸치노, 차가 있어요. 무엇을 주문하시겠어요?",
                3,
                false);
    }

    @Test
    void answerWithoutFilledSlotsDoesNotDeductHeart() throws Exception {
        assertTurnPolicy(
                "mvp2-sub-4|answer-empty@example.com|Answer Empty User",
                "That’s all.",
                TurnClassification.ANSWER,
                List.of(),
                "What drink would you like to order?",
                "어떤 음료를 주문하고 싶으신가요?",
                3,
                false);
    }

    @Test
    void answerWithFilledSlotDoesNotDeductHeart() throws Exception {
        assertTurnPolicy(
                "mvp2-sub-5|answer-filled@example.com|Answer Filled User",
                "I want coffee.",
                TurnClassification.ANSWER,
                List.of(new AiFilledSlot("drink")),
                "What size would you like?",
                "어떤 사이즈로 드릴까요?",
                3,
                false);
    }

    @Test
    void invalidResponseDeductsHeart() throws Exception {
        assertTurnPolicy(
                "mvp2-sub-6|invalid@example.com|Invalid User",
                "I want drink.",
                TurnClassification.INVALID_RESPONSE,
                List.of(),
                "What drink would you like to order?",
                "어떤 음료를 주문하고 싶으신가요?",
                2,
                true);
    }

    @Test
    void submitUtteranceRejectsBlankUserUtterance() throws Exception {
        String accessToken = login("mvp2-sub-7|blank@example.com|Blank User");
        long sessionId = startSession(accessToken, 4);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void submitUtteranceRejectsCompletedSession() throws Exception {
        String accessToken = login("mvp2-sub-8|completed@example.com|Completed User");
        long sessionId = startSession(accessToken, 4);
        completeSession(accessToken, sessionId);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"One more answer."}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SESSION_ALREADY_COMPLETED"));
    }

    @Test
    void submitUtteranceRejectsOtherUsersSession() throws Exception {
        String ownerAccessToken = login("mvp2-sub-9|owner@example.com|Owner User");
        String otherAccessToken = login("mvp2-sub-10|other@example.com|Other User");
        long sessionId = startSession(ownerAccessToken, 4);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I want an iced americano."}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/result", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherAccessToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void sessionResultReturnsSuccessWhenSessionSucceeded() throws Exception {
        String accessToken = login("mvp2-sub-11|result-success@example.com|Result Success User");
        long sessionId = startSession(accessToken, 4);
        completeSession(accessToken, sessionId);

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/result", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scenarioResult").value("SUCCESS"))
                .andExpect(jsonPath("$.data.sessionId").doesNotExist())
                .andExpect(jsonPath("$.data.remainingHearts").doesNotExist())
                .andExpect(jsonPath("$.data.feedbackAvailable").doesNotExist());
    }

    @Test
    void sessionResultReturnsFailureWhenSessionFailed() throws Exception {
        String accessToken = login("mvp2-sub-12|result-failure@example.com|Result Failure User");
        long sessionId = startSession(accessToken, 4);
        for (int index = 0; index < 3; index++) {
            aiConversationClient.enqueueNextQuestion(new AiNextQuestionResponse(
                    "What drink would you like to order?",
                    "어떤 음료를 주문하고 싶으신가요?",
                    List.of(),
                    TurnClassification.INVALID_RESPONSE));
        }

        for (int index = 0; index < 3; index++) {
            mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"userUtterance":"I want drink."}
                                    """))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/result", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scenarioResult").value("FAILURE"));
    }

    @Test
    void sessionResultRejectsInProgressSession() throws Exception {
        String accessToken = login("mvp2-sub-13|result-progress@example.com|Result Progress User");
        long sessionId = startSession(accessToken, 4);

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/result", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SESSION_NOT_COMPLETABLE"));
    }

    @Test
    void scenarioApisRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/scenarios"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/scenarios/1/sessions"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/sessions/1/result"))
                .andExpect(status().isUnauthorized());
    }

    private long startSession(String accessToken, long scenarioId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/sessions", scenarioId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sessionId").isNumber())
                .andExpect(jsonPath("$.data.originalQuestion").value("Hi, what's the purpose of your visit?"))
                .andExpect(jsonPath("$.data.translatedQuestion").value("안녕하세요. 방문 목적이 어떻게 되시나요?"))
                .andExpect(jsonPath("$.data.remainingHearts").value(3))
                .andExpect(jsonPath("$.data.feedbackAvailable").value(false))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return body.get("data").get("sessionId").asLong();
    }

    private void assertTurnPolicy(
            String idToken,
            String userUtterance,
            TurnClassification turnClassification,
            List<AiFilledSlot> filledSlots,
            String nextQuestion,
            String translatedQuestion,
            int expectedRemainingHearts,
            boolean expectedHeartDeducted
    ) throws Exception {
        String accessToken = login(idToken);
        long sessionId = startSession(accessToken, 4);
        aiConversationClient.enqueueNextQuestion(new AiNextQuestionResponse(
                nextQuestion,
                translatedQuestion,
                filledSlots,
                turnClassification));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"%s"}
                                """.formatted(userUtterance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.remainingHearts").value(expectedRemainingHearts))
                .andExpect(jsonPath("$.data.feedbackAvailable").value(false))
                .andExpect(jsonPath("$.data.heartDeducted").value(expectedHeartDeducted))
                .andExpect(jsonPath("$.data.turnClassification").value(turnClassification.name()))
                .andExpect(jsonPath("$.data.originalQuestion").value(nextQuestion))
                .andExpect(jsonPath("$.data.translatedQuestion").value(translatedQuestion));
    }

    private void completeSession(String accessToken, long sessionId) throws Exception {
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
    static class ScenarioFlowTestConfiguration {

        @Bean
        @Primary
        TestAiConversationClient testAiConversationClient() {
            return new TestAiConversationClient();
        }
    }

    static class TestAiConversationClient implements AiConversationClient {

        private final Queue<AiNextQuestionResponse> nextQuestionResponses = new ArrayDeque<>();

        void reset() {
            nextQuestionResponses.clear();
        }

        void enqueueNextQuestion(AiNextQuestionResponse response) {
            nextQuestionResponses.add(response);
        }

        @Override
        public AiNextQuestionResponse generateNextQuestion(AiNextQuestionRequest request) {
            AiNextQuestionResponse queuedResponse = nextQuestionResponses.poll();
            if (queuedResponse != null) {
                return queuedResponse;
            }

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
    }
}

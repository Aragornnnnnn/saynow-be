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
                .andExpect(jsonPath("$.data.categories[0].categoryLocked").value(false))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].locked").value(false))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].locked").value(true))
                .andExpect(jsonPath("$.data.categories[1].categoryLocked").value(true))
                .andExpect(jsonPath("$.data.categories[1].categoryLockReason").value("COMING_SOON"));

        mockMvc.perform(post("/api/v1/scenarios/2/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SCENARIO_LOCKED"));

        long sessionId = startSession(accessToken, 1);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I want an iced americano."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.remainingHearts").value(3))
                .andExpect(jsonPath("$.data.feedbackAvailable").value(false))
                .andExpect(jsonPath("$.data.originalQuestion").value("Could you tell me your size?"));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"Medium size, please."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.remainingHearts").value(3))
                .andExpect(jsonPath("$.data.feedbackAvailable").value(true))
                .andExpect(jsonPath("$.data.originalQuestion").value(nullValue()))
                .andExpect(jsonPath("$.data.translatedQuestion").value(nullValue()));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/feedback", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.cleared").value(true))
                .andExpect(jsonPath("$.data.comprehensionScore").value(82))
                .andExpect(jsonPath("$.data.remainingHearts").value(3))
                .andExpect(jsonPath("$.data.turnFeedbacks.length()").value(2))
                .andExpect(jsonPath("$.data.turnFeedbacks[0].sequence").value(1))
                .andExpect(jsonPath("$.data.turnFeedbacks[0].userUtterance").value("I want an iced americano."));

        mockMvc.perform(get("/api/v1/scenarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].cleared").value(true))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].locked").value(false));
    }

    @Test
    void emptyFilledSlotsDecreasesHeart() throws Exception {
        String accessToken = login("mvp2-sub-2|empty@example.com|Empty Slots User");
        long sessionId = startSession(accessToken, 1);
        aiConversationClient.enqueueNextQuestion(new AiNextQuestionResponse(
                "Could you tell me what drink you want?",
                "원하는 음료를 말씀해 주시겠어요?",
                List.of()));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"The weather is nice today."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.remainingHearts").value(2))
                .andExpect(jsonPath("$.data.feedbackAvailable").value(false))
                .andExpect(jsonPath("$.data.originalQuestion").value("Could you tell me what drink you want?"));
    }

    @Test
    void submitUtteranceRejectsBlankUserUtterance() throws Exception {
        String accessToken = login("mvp2-sub-3|blank@example.com|Blank User");
        long sessionId = startSession(accessToken, 1);

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
        String accessToken = login("mvp2-sub-4|completed@example.com|Completed User");
        long sessionId = startSession(accessToken, 1);
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
        String ownerAccessToken = login("mvp2-sub-5|owner@example.com|Owner User");
        String otherAccessToken = login("mvp2-sub-6|other@example.com|Other User");
        long sessionId = startSession(ownerAccessToken, 1);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I want an iced americano."}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void scenarioApisRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/scenarios"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/scenarios/1/sessions"))
                .andExpect(status().isUnauthorized());
    }

    private long startSession(String accessToken, long scenarioId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/sessions", scenarioId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sessionId").isNumber())
                .andExpect(jsonPath("$.data.originalQuestion").value("What would you like to order?"))
                .andExpect(jsonPath("$.data.translatedQuestion").value("무엇을 주문하시겠어요?"))
                .andExpect(jsonPath("$.data.remainingHearts").value(3))
                .andExpect(jsonPath("$.data.feedbackAvailable").value(false))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return body.get("data").get("sessionId").asLong();
    }

    private void completeSession(String accessToken, long sessionId) throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I want an iced americano."}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"Medium size, please."}
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
                return new AiNextQuestionResponse(null, null, newlyFilled);
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
                    newlyFilled);
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

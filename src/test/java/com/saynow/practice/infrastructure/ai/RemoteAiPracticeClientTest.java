// 원격 AI 클라이언트의 multipart 요청과 응답 매핑을 검증하는 테스트
package com.saynow.practice.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.practice.domain.InputType;
import com.saynow.practice.domain.SessionStatus;
import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.ScenarioCategory;
import com.saynow.scenario.domain.ScenarioSlot;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RemoteAiPracticeClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsMultipartTurnEvaluationRequestAndMapsResponse() throws Exception {
        AtomicReference<RecordedRequest> recordedRequest = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/turn-evaluations", exchange -> handleTurnEvaluation(exchange, recordedRequest));
        server.start();

        try {
            int port = server.getAddress().getPort();
            RemoteAiPracticeClient client = new RemoteAiPracticeClient(
                    objectMapper,
                    new AiClientProperties(URI.create("http://127.0.0.1:" + port), "remote", "/api/v1/turn-evaluations"));

            AiTurnEvaluationResult result = client.evaluateTurn(new AiTurnEvaluationRequest(
                    "session-1",
                    scenario(),
                    "Hi! What would you like to order?",
                    null,
                    1,
                    "turn-1.webm",
                    "audio/webm",
                    "audio-content".getBytes(),
                    InputType.AUDIO,
                    2100,
                    3600,
                    List.of(slot("drink", "주문할 음료")),
                    Map.of("temperature", "iced"),
                    0,
                    5));

            RecordedRequest request = recordedRequest.get();
            assertThat(request).isNotNull();
            assertThat(request.method()).isEqualTo("POST");
            assertThat(request.contentType()).startsWith("multipart/form-data; boundary=");
            assertThat(new String(request.body(), StandardCharsets.UTF_8))
                    .contains("name=\"audio\"; filename=\"turn-1.webm\"")
                    .contains("Content-Type: audio/webm")
                    .contains("name=\"payload\"")
                    .contains("\"sessionId\":\"session-1\"")
                    .contains("\"scenarioId\":\"cafe_iced_americano\"")
                    .contains("\"requiredSlots\":[{\"slotKey\":\"drink\",\"description\":\"주문할 음료\"}]")
                    .contains("\"currentQuestion\":{\"questionText\":\"Hi! What would you like to order?\"}")
                    .contains("\"turn\":{\"turnIndex\":1,\"inputType\":\"AUDIO\"");

            assertThat(result.transcript()).isEqualTo("I want an iced americano");
            assertThat(result.sttConfidence()).isEqualByComparingTo(new BigDecimal("0.86"));
            assertThat(result.scenarioStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
            assertThat(result.filledSlots()).containsExactly(new AiFilledSlot("drink", "americano"));
            assertThat(result.nextPrompt()).isEqualTo(new AiPrompt("Would you like that hot or iced?", null));
            assertThat(result.resultMessage()).isNull();
        } finally {
            server.stop(0);
        }
    }

    private void handleTurnEvaluation(HttpExchange exchange, AtomicReference<RecordedRequest> recordedRequest) throws IOException {
        recordedRequest.set(new RecordedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestHeaders().getFirst("Content-Type"),
                exchange.getRequestBody().readAllBytes()));

        byte[] response = """
                        {
                          "transcript": "I want an iced americano",
                          "sttConfidence": 0.86,
                          "scenarioStatus": "IN_PROGRESS",
                          "filledSlots": [
                            {
                              "slotKey": "drink",
                              "slotValue": "americano"
                            }
                          ],
                          "nextQuestion": {
                            "questionText": "Would you like that hot or iced?",
                            "messageText": null,
                            "ttsAudio": "ignored-base64"
                          },
                          "resultMessage": null
                        }
                        """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private Scenario scenario() {
        ScenarioCategory category = mock(ScenarioCategory.class);
        when(category.getCategoryKey()).thenReturn("cafe");

        Scenario scenario = mock(Scenario.class);
        when(scenario.getCategory()).thenReturn(category);
        when(scenario.getScenarioKey()).thenReturn("cafe_iced_americano");
        when(scenario.getTitle()).thenReturn("아이스 아메리카노 주문하기");
        when(scenario.getSituationDescription()).thenReturn("카페에서 원하는 음료를 주문해야 합니다.");
        when(scenario.getSuccessGoal()).thenReturn("아이스 아메리카노 주문에 성공하세요.");
        when(scenario.getMaxFollowUpCount()).thenReturn(5);
        return scenario;
    }

    private ScenarioSlot slot(String slotKey, String description) {
        ScenarioSlot slot = mock(ScenarioSlot.class);
        when(slot.getSlotKey()).thenReturn(slotKey);
        when(slot.getDescription()).thenReturn(description);
        return slot;
    }

    private record RecordedRequest(
            String method,
            String contentType,
            byte[] body
    ) {
    }
}

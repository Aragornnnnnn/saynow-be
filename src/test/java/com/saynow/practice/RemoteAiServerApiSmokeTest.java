// 실제 AI 서버와 백엔드 세션 API 연동을 검증하는 수동 smoke test
package com.saynow.practice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.practice.domain.InputType;
import com.saynow.practice.domain.PracticeSession;
import com.saynow.practice.domain.PracticeTurn;
import com.saynow.practice.domain.SessionStatus;
import com.saynow.practice.infrastructure.ai.AiPracticeClient;
import com.saynow.practice.infrastructure.ai.AiSessionFeedbackRequest;
import com.saynow.practice.infrastructure.ai.AiSessionFeedbackResult;
import com.saynow.scenario.domain.Scenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:saynow-remote-ai-smoke;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "saynow.ai.client-mode=remote",
        "saynow.ai.base-url=http://43.202.146.182:8080"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "SAYNOW_REMOTE_AI_SMOKE_TEST", matches = "true")
class RemoteAiServerApiSmokeTest {

    private static final Path AUDIO_PATH = Path.of("/private/tmp/saynow_turn.mp3");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AiPracticeClient aiPracticeClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void submitsTurnThroughBackendApiToRemoteAiServer() throws Exception {
        assertThat(Files.exists(AUDIO_PATH)).as("smoke test audio file").isTrue();

        MvcResult startResult = mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioId":"cafe_iced_americano"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode startBody = objectMapper.readTree(startResult.getResponse().getContentAsByteArray());
        String sessionId = startBody.get("data").get("sessionId").asText();

        MockMultipartFile audio = new MockMultipartFile(
                "audio",
                "turn-1.mp3",
                "audio/mpeg",
                Files.readAllBytes(AUDIO_PATH));

        MvcResult turnResult = mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio)
                        .file(turnRequest("AUDIO", 1000, 2000)))
                .andReturn();
        assertThat(turnResult.getResponse().getStatus())
                .describedAs(turnFailureMessage(turnResult))
                .isEqualTo(200);

        JsonNode turnBody = objectMapper.readTree(turnResult.getResponse().getContentAsByteArray());
        assertThat(turnBody.get("data").get("transcript").asText()).containsIgnoringCase("americano");
        assertThat(turnBody.get("data").get("status").asText()).isIn("IN_PROGRESS", "SUCCESS");
    }

    @Test
    void createsSessionFeedbackThroughRemoteAiServer() {
        AiSessionFeedbackResult result = aiPracticeClient.createSessionFeedback(new AiSessionFeedbackRequest(
                "smoke-session-1",
                scenario(),
                SessionStatus.SUCCESS,
                java.util.Map.of("drink", "americano", "temperature", "iced"),
                List.of(
                        turn(1, "Hi! What can I get for you today?", "I want an iced americano.", 2100),
                        turn(2, "Would you like that iced or hot?", "Iced, please.", 1700))));

        assertThat(result.totalUnderstoodScore()).isBetween(0, 100);
        assertThat(result.summary()).isNotBlank();
        assertThat(result.turnFeedback()).hasSize(2);
        assertThat(result.turnFeedback().get(0).heardAs()).isNotBlank();
        assertThat(result.turnFeedback().get(0).reason()).isNotBlank();
    }

    private String turnFailureMessage(MvcResult result) throws UnsupportedEncodingException {
        return "body=%s, exception=%s".formatted(
                result.getResponse().getContentAsString(),
                exceptionSummary(result.getResolvedException()));
    }

    private String exceptionSummary(Throwable exception) {
        if (exception == null) {
            return "none";
        }

        StringBuilder summary = new StringBuilder();
        Throwable current = exception;
        while (current != null) {
            if (!summary.isEmpty()) {
                summary.append(" <- ");
            }
            summary.append(current.getClass().getName());
            if (current.getMessage() != null) {
                summary.append(": ").append(current.getMessage());
            }
            current = current.getCause();
        }
        return summary.toString();
    }

    private Scenario scenario() {
        Scenario scenario = mock(Scenario.class);
        when(scenario.getScenarioKey()).thenReturn("cafe_1");
        when(scenario.getTitle()).thenReturn("아이스 아메리카노 주문하기");
        when(scenario.getSituationDescription()).thenReturn("카페에서 원하는 음료를 주문해야 합니다.");
        when(scenario.getSuccessGoal()).thenReturn("아이스 아메리카노 주문에 성공하세요.");
        return scenario;
    }

    private PracticeTurn turn(int turnIndex, String questionText, String transcript, Integer speechStartedAfterMs) {
        PracticeTurn turn = new PracticeTurn(
                mock(PracticeSession.class),
                turnIndex,
                questionText,
                null,
                transcript,
                InputType.AUDIO,
                speechStartedAfterMs,
                3000,
                new BigDecimal("0.91"));
        ReflectionTestUtils.setField(turn, "id", (long) turnIndex);
        return turn;
    }

    private MockMultipartFile turnRequest(String inputType, int speechStartedAfterMs, int recordingDurationMs) {
        return new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                """
                        {
                          "inputType":"%s",
                          "speechStartedAfterMs":%d,
                          "recordingDurationMs":%d
                        }
                        """.formatted(inputType, speechStartedAfterMs, recordingDurationMs)
                        .getBytes(StandardCharsets.UTF_8));
    }
}

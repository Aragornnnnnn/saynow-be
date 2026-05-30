// 요청 추적 로그와 핵심 비즈니스 로그를 통합 검증한다.
package com.saynow.common.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.IntegrationTestSupport;
import com.saynow.feedback.application.FeedbackService;
import com.saynow.session.application.SessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ObservabilityIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<AttachedAppender> attachedAppenders = new ArrayList<>();

    @AfterEach
    void detachAppenders() {
        attachedAppenders.forEach(AttachedAppender::detach);
        attachedAppenders.clear();
    }

    @Test
    void requestLogIncludesTraceFieldsAndAuthenticatedUserId() throws Exception {
        ListAppender<ILoggingEvent> requestLogs = attachListAppender("com.saynow.common.web.RequestLoggingFilter");
        String accessToken = login("observability-request-sub|observability-request@example.com|Observability Request");

        mockMvc.perform(get("/api/v1/scenarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());

        assertThat(requestLogs.list)
                .anySatisfy(event -> {
                    String message = event.getFormattedMessage();
                    assertThat(message)
                            .contains("HTTP 요청 처리를 완료했습니다.")
                            .contains("method=GET")
                            .contains("path=/api/v1/scenarios")
                            .contains("status=200")
                            .containsPattern("durationMs=\\d+")
                            .containsPattern("userId=\\d+");
                    assertThat(event.getMDCPropertyMap()).containsKey("requestId");
                });
    }

    @Test
    void sessionLogsIncludeBusinessStateWithoutUserUtteranceBody() throws Exception {
        ListAppender<ILoggingEvent> sessionLogs = attachListAppender(SessionService.class);
        String accessToken = login("observability-session-sub|observability-session@example.com|Observability Session");

        long sessionId = startSession(accessToken, 4);
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I'm here for sightseeing."}
                                """))
                .andExpect(status().isOk());

        assertThat(sessionLogs.list)
                .anySatisfy(event -> {
                    assertThat(event.getFormattedMessage())
                            .contains("세션을 시작했습니다.")
                            .contains("scenarioId=4")
                            .containsPattern("sessionId=\\d+")
                            .containsPattern("userId=\\d+");
                    assertThat(event.getMDCPropertyMap()).containsKeys("requestId", "userId");
                });
        assertThat(sessionLogs.list)
                .anySatisfy(event -> assertThat(event.getFormattedMessage())
                        .contains("세션 발화를 처리했습니다.")
                        .contains("sessionId=" + sessionId)
                        .contains("turnClassification=ANSWER")
                        .contains("heartDeducted=false")
                        .contains("remainingHearts=3")
                        .doesNotContain("I'm here for sightseeing."));
    }

    @Test
    void feedbackLogsIncludeCompletionSummary() throws Exception {
        ListAppender<ILoggingEvent> feedbackLogs = attachListAppender(FeedbackService.class);
        String accessToken = login("observability-feedback-sub|observability-feedback@example.com|Observability Feedback");
        long sessionId = completeSession(accessToken);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/feedback", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());

        assertThat(feedbackLogs.list)
                .anySatisfy(event -> assertThat(event.getFormattedMessage())
                        .contains("세션 피드백 생성을 완료했습니다.")
                        .contains("sessionId=" + sessionId)
                        .contains("cleared=true")
                        .contains("turnCount=3"));
    }

    private ListAppender<ILoggingEvent> attachListAppender(Class<?> loggerType) {
        return attachListAppender(loggerType.getName());
    }

    private ListAppender<ILoggingEvent> attachListAppender(String loggerName) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        attachedAppenders.add(new AttachedAppender(logger, appender));
        return appender;
    }

    private long completeSession(String accessToken) throws Exception {
        long sessionId = startSession(accessToken, 4);
        submitUtterance(accessToken, sessionId, "I'm here for sightseeing.");
        submitUtterance(accessToken, sessionId, "I'll stay for five days.");
        submitUtterance(accessToken, sessionId, "I'll stay at the Midtown Hotel.");
        return sessionId;
    }

    private long startSession(String accessToken, long scenarioId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/sessions", scenarioId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return body.get("data").get("sessionId").asLong();
    }

    private void submitUtterance(String accessToken, long sessionId, String userUtterance) throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"%s"}
                                """.formatted(userUtterance)))
                .andExpect(status().isOk());
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

    private record AttachedAppender(Logger logger, ListAppender<ILoggingEvent> appender) {

        private void detach() {
            logger.detachAppender(appender);
        }
    }
}

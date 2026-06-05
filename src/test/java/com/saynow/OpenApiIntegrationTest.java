// OpenAPI 문서가 3차 MVP API 경로와 공통 응답 예시를 노출하는지 검증한다.
package com.saynow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesOpenApiDocumentForMvp3Apis() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("SayNow Backend API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.servers[0].url").value("https://saynow.p-e.kr"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/auth/social-login'].post.security.length()").value(0))
                .andExpect(jsonPath("$.paths['/api/v1/auth/token/refresh'].post.security.length()").value(0))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/auth/social-login")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/auth/token/refresh")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/auth/logout")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/scenarios")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/scenarios/{scenarioId}/sessions")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/sessions/{sessionId}/utterances")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/sessions/{sessionId}/feedback")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/sessions/{sessionId}/abandon")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/sessions/{sessionId}/nps")))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/guide']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/result']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback/stream']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}']").doesNotExist())
                .andExpect(jsonPath("$.tags[?(@.name == 'Scenario')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Session')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Feedback')]").exists());
    }

    @Test
    void exposesSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void separatesSuccessAndErrorResponsesPerApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios'].get.responses.200.content['application/json'].examples.SUCCESS.value.success").value(true))
                .andExpect(jsonPath("$.paths['/api/v1/scenarios'].get.responses.200.content['application/json'].examples.SUCCESS.value.error").value(nullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/scenarios'].get.responses.200.content['application/json'].examples.SUCCESS.value.data.categories[0].categoryName").value("Free Talk"))
                .andExpect(jsonPath("$.paths['/api/v1/scenarios'].get.responses.200.content['application/json'].examples.SUCCESS.value.data.categories[0].scenarios[0].briefing").isString())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios'].get.responses.200.content['application/json'].examples.SUCCESS.value.data.categories[0].scenarios[0].conversationGoal").isString())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios'].get.responses.200.content['application/json'].examples.SUCCESS.value.data.categories[0].scenarios[0].firstQuestionPreview.aiQuestion").value("What is your favorite food? Why do you like it?"))
                .andExpect(jsonPath("$.paths['/api/v1/scenarios'].get.responses.401.content['application/json'].examples.AUTH_REQUIRED.value.error.code").value("AUTH_REQUIRED"))
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/sessions'].post.responses.201.content['application/json'].examples.SUCCESS.value.data.currentTurn.sequence").value(1))
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/sessions'].post.responses.403.content['application/json'].examples.SCENARIO_LOCKED.value.error.code").value("SCENARIO_LOCKED"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.submittedTurn.turnFeedbackStatus").value("PREPARING"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.nextTurn.sequence").value(2))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.400.content['application/json'].examples.INVALID_REQUEST.value.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.403.content['application/json'].examples.FORBIDDEN.value.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.409.content['application/json'].examples.SESSION_ALREADY_COMPLETED.value.error.code").value("SESSION_ALREADY_COMPLETED"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.nativeScore").value(82))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.nativeScoreBreakdown").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.highlightMessage").value("한국인의 40%가 헷갈리는 간접의문문 어순을 피해 간 사람이에요."))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.nativeLevelLabel").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.summary").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[0].feedbackType").value("GOOD"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[0].originalQuestion").value("What is your favorite food? Why do you like it?"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[0].positiveFeedback").value(nullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[0].feedbackDetail").isString())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[0].benchmarkMessage").isString())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[0].plusOneExpression").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[0].betterExpression").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.409.content['application/json'].examples.SESSION_NOT_COMPLETED.value.error.code").value("SESSION_NOT_COMPLETED"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.409.content['application/json'].examples.FEEDBACK_NOT_READY.value.error.code").value("FEEDBACK_NOT_READY"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.503.content['application/json'].examples.FEEDBACK_GENERATION_FAILED.value.error.code").value("FEEDBACK_GENERATION_FAILED"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/abandon'].patch.responses.200.content['application/json'].examples.SUCCESS.value.data").value(nullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/nps'].post.responses.201.content['application/json'].examples.SUCCESS.value.data").value(nullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/nps'].post.responses.409.content['application/json'].examples.NPS_ALREADY_SUBMITTED.value.error.code").value("NPS_ALREADY_SUBMITTED"));
    }
}

// OpenAPI 문서가 2차 MVP API 경로와 공통 응답 예시를 노출하는지 검증한다.
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
    void exposesOpenApiDocumentForMvp2Apis() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("SayNow Backend API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.servers[0].url").value("https://saynow.p-e.kr"))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/auth/social-login")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/auth/token/refresh")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/auth/logout")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/scenarios")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/scenarios/{scenarioId}/sessions")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/sessions/{sessionId}/utterances")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/sessions/{sessionId}/feedback")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/sessions/{sessionId}")))
                .andExpect(jsonPath("$.paths['/api/v1/categories']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/sessions']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/turns']").doesNotExist())
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
                .andExpect(jsonPath("$.paths['/api/v1/scenarios'].get.responses.401.content['application/json'].examples.AUTH_REQUIRED.value.error.code").value("AUTH_REQUIRED"))
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/sessions'].post.responses.201.content['application/json'].examples.SUCCESS.value.data.feedbackAvailable").value(false))
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/sessions'].post.responses.403.content['application/json'].examples.SCENARIO_LOCKED.value.error.code").value("SCENARIO_LOCKED"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.originalQuestion").value("What size would you like?"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.400.content['application/json'].examples.INVALID_REQUEST.value.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.403.content['application/json'].examples.FORBIDDEN.value.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.409.content['application/json'].examples.SESSION_ALREADY_COMPLETED.value.error.code").value("SESSION_ALREADY_COMPLETED"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[0].userUtterance").value("I want iced americano."))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.503.content['application/json'].examples.FEEDBACK_GENERATION_FAILED.value.error.code").value("FEEDBACK_GENERATION_FAILED"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}'].delete.responses.200.content['application/json'].examples.SUCCESS.value.data").value(nullValue()));
    }
}

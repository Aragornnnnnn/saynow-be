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
    void exposesOpenApiDocumentForMvpApis() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("SayNow Backend API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/categories")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/sessions")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/sessions/{sessionId}/turns")))
                .andExpect(jsonPath("$.tags[?(@.name == 'Scenario')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Practice Session')]").exists())
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
                .andExpect(jsonPath("$.paths['/api/v1/categories'].get.responses.200").exists())
                .andExpect(jsonPath("$.paths['/api/v1/categories'].get.responses.200.content['application/json'].examples.SUCCESS.value.success").value(true))
                .andExpect(jsonPath("$.paths['/api/v1/categories'].get.responses.200.content['application/json'].examples.SUCCESS.value.error").value(nullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/categories'].get.responses.500").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios'].get.responses.200").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios'].get.responses.404.content['application/json'].examples.CATEGORY_NOT_FOUND.value.error.code").value("CATEGORY_NOT_FOUND"))
                .andExpect(jsonPath("$.paths['/api/v1/categories/{categoryId}/scenarios']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}'].get.responses.404.content['application/json'].examples.SCENARIO_NOT_FOUND.value.error.code").value("SCENARIO_NOT_FOUND"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions'].post.responses.200").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/sessions'].post.responses.201").exists())
                .andExpect(jsonPath("$.paths['/api/v1/sessions'].post.responses.201.content['application/json'].examples.SUCCESS.value.success").value(true))
                .andExpect(jsonPath("$.paths['/api/v1/sessions'].post.responses.201.content['application/json'].examples.SUCCESS.value.error").value(nullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/sessions'].post.responses.400.content['application/json'].examples.VALIDATION_FAILED.value.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions'].post.responses.404.content['application/json'].examples.SCENARIO_NOT_FOUND.value.error.code").value("SCENARIO_NOT_FOUND"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}'].get.responses.404.content['application/json'].examples.SESSION_NOT_FOUND.value.error.code").value("SESSION_NOT_FOUND"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/micReady'].put.responses.409.content['application/json'].examples.SESSION_ALREADY_ENDED.value.error.code").value("SESSION_ALREADY_ENDED"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/turns'].post.responses.413.content['application/json'].examples.AUDIO_TOO_LARGE.value.error.code").value("AUDIO_TOO_LARGE"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/turns'].post.responses.415.content['application/json'].examples.UNSUPPORTED_AUDIO_TYPE.value.error.code").value("UNSUPPORTED_AUDIO_TYPE"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/turns'].post.responses.503.content['application/json'].examples.AI_STT_FAILED.value.error.code").value("AI_STT_FAILED"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/exit'].post.responses.409.content['application/json'].examples.SESSION_ALREADY_ENDED.value.error.code").value("SESSION_ALREADY_ENDED"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].get.responses.400.content['application/json'].examples.SESSION_IN_PROGRESS.value.error.code").value("SESSION_IN_PROGRESS"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].get.responses.404.content['application/json'].examples.FEEDBACK_NOT_FOUND.value.error.code").value("FEEDBACK_NOT_FOUND"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].get.responses.500.content['application/json'].examples.INTERNAL_SERVER_ERROR.value.error.code").value("INTERNAL_SERVER_ERROR"));
    }
}

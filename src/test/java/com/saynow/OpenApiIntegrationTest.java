package com.saynow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
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
}

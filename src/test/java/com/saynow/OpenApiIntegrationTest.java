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
                .andExpect(jsonPath("$.paths['/api/v1/app-versions/check'].get.security.length()").value(0))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/auth/social-login")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/auth/token/refresh")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/auth/logout")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/app-versions/check")))
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
                .andExpect(jsonPath("$.tags[?(@.name == 'Feedback')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'App Version')]").exists());
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
                .andExpect(jsonPath("$.paths['/api/v1/app-versions/check'].get.responses.200.content['application/json'].examples.SUCCESS.value.data.updateType").value("SOFT"))
                .andExpect(jsonPath("$.paths['/api/v1/app-versions/check'].get.responses.200.content['application/json'].examples.SUCCESS.value.data.latestVersionName").value("1.4.0"))
                .andExpect(jsonPath("$.paths['/api/v1/app-versions/check'].get.responses.200.content['application/json'].examples.SUCCESS.value.data.storeUrl").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/app-versions/check'].get.responses.400.content['application/json'].examples.VALIDATION_FAILED.value.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.paths['/api/v1/scenarios'].get.responses.200.content['application/json'].examples.SUCCESS.value.success").value(true))
                .andExpect(jsonPath("$.paths['/api/v1/scenarios'].get.responses.200.content['application/json'].examples.SUCCESS.value.error").value(nullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/scenarios'].get.responses.200.content['application/json'].examples.SUCCESS.value.data.categories[0].categoryName").value("룸메이트"))
                .andExpect(jsonPath("$.paths['/api/v1/scenarios'].get.responses.200.content['application/json'].examples.SUCCESS.value.data.categories[0].scenarios[0].briefing").isString())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios'].get.responses.200.content['application/json'].examples.SUCCESS.value.data.categories[0].scenarios[0].conversationGoal").isString())
                .andExpect(jsonPath("$.paths['/api/v1/scenarios'].get.responses.200.content['application/json'].examples.SUCCESS.value.data.categories[0].scenarios[0].firstQuestionPreview.aiQuestion").value("Hey, you're my roommate, right?! I'm Charlie, nice to meet you! What's your name? Tell me a little about yourself!"))
                .andExpect(jsonPath("$.paths['/api/v1/scenarios'].get.responses.401.content['application/json'].examples.AUTH_REQUIRED.value.error.code").value("AUTH_REQUIRED"))
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/sessions'].post.responses.201.content['application/json'].examples.SUCCESS.value.data.currentTurn.sequence").value(1))
                .andExpect(jsonPath("$.paths['/api/v1/scenarios/{scenarioId}/sessions'].post.responses.403.content['application/json'].examples.SCENARIO_LOCKED.value.error.code").value("SCENARIO_LOCKED"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.submittedTurn.turnFeedbackStatus").value("PREPARING"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.submittedTurn.innerThought").isString())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.submittedTurn.innerThoughtType").value("GOOD"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.nextTurn.sequence").value(2))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.200.content['application/json'].examples.COMPLETED.value.data.submittedTurn.sequence").value(3))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.200.content['application/json'].examples.COMPLETED.value.data.submittedTurn.innerThought").value("한국에서 가볼 만한 곳을 이유까지 말해줘서 대화가 훨씬 살아난다."))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.200.content['application/json'].examples.COMPLETED.value.data.submittedTurn.innerThoughtType").value("GOOD"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.200.content['application/json'].examples.COMPLETED.value.data.nextTurn.sequence").value(4))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.200.content['application/json'].examples.COMPLETED.value.data.nextTurn.aiQuestion").value("That sounds amazing. I’ll definitely add it to my Korea list."))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.200.content['application/json'].examples.COMPLETED.value.data.nextTurn.translatedQuestion").value("정말 좋다. 한국에서 가볼 곳 리스트에 꼭 넣어둘게."))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.200.content['application/json'].examples.COMPLETED.value.data.progress.currentSequence").value(4))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.200.content['application/json'].examples.COMPLETED.value.data.progress.completed").value(true))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.400.content['application/json'].examples.INVALID_REQUEST.value.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.403.content['application/json'].examples.FORBIDDEN.value.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.409.content['application/json'].examples.SESSION_ALREADY_COMPLETED.value.error.code").value("SESSION_ALREADY_COMPLETED"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/utterances'].post.responses.503.content['application/json'].examples.AI_GENERATION_FAILED.value.error.code").value("AI_GENERATION_FAILED"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.nativeScore").value(82))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.nativeScoreBreakdown").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.highlightMessage").value("한국인의 40%가 헷갈리는 간접의문문 어순을 피해 간 사람이에요."))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.nativeLevelLabel").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.summary").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[0].feedbackType").value("GOOD"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[0].originalQuestion").value("Hey, you're my roommate, right?! I'm Charlie, nice to meet you! What's your name? Tell me a little about yourself!"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[0].positiveFeedback").value(nullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[0].feedbackDetail").isString())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[0].correctionExpression").value(nullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[0].correctionReason").value(nullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[0].benchmarkMessage").isString())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[1].feedbackType").value("NEEDS_IMPROVEMENT"))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[1].feedbackDetail").value(nullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[1].correctionExpression").isString())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{sessionId}/feedback'].post.responses.200.content['application/json'].examples.SUCCESS.value.data.turnFeedbacks[1].correctionReason").isString())
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

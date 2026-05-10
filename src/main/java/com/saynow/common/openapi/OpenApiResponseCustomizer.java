package com.saynow.common.openapi;

import com.saynow.common.exception.ErrorCode;
import com.saynow.auth.api.AuthController;
import com.saynow.feedback.api.FeedbackController;
import com.saynow.practice.api.PracticeSessionController;
import com.saynow.scenario.api.ScenarioController;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class OpenApiResponseCustomizer {

    private static final String APPLICATION_JSON = "application/json";
    private static final Map<String, EndpointDoc> ENDPOINT_DOCS = Map.ofEntries(
            endpoint(AuthController.class, "socialLogin",
                    success(HttpStatus.OK, "소셜 로그인 성공", objectMap(
                            "tokenType", "Bearer",
                            "accessToken", "saynow-access-token",
                            "accessTokenExpiresIn", 1800,
                            "refreshToken", "saynow-refresh-token",
                            "refreshTokenExpiresIn", 1209600,
                            "member", objectMap(
                                    "memberId", "1",
                                    "nickname", "Ryan",
                                    "email", "ryan@example.com",
                                    "provider", "GOOGLE",
                                    "newMember", true)
                    )),
                    errors(
                            error(ErrorCode.VALIDATION_FAILED),
                            error(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER),
                            error(ErrorCode.OIDC_TOKEN_INVALID),
                            error(ErrorCode.OIDC_NONCE_MISMATCH),
                            error(ErrorCode.OIDC_PROVIDER_UNAVAILABLE),
                            error(ErrorCode.INTERNAL_SERVER_ERROR))),
            endpoint(AuthController.class, "refresh",
                    success(HttpStatus.OK, "토큰 재발급 성공", objectMap(
                            "tokenType", "Bearer",
                            "accessToken", "new-saynow-access-token",
                            "accessTokenExpiresIn", 1800,
                            "refreshToken", "new-saynow-refresh-token",
                            "refreshTokenExpiresIn", 1209600
                    )),
                    errors(
                            error(ErrorCode.VALIDATION_FAILED),
                            error(ErrorCode.REFRESH_TOKEN_INVALID),
                            error(ErrorCode.INTERNAL_SERVER_ERROR))),
            endpoint(AuthController.class, "logout",
                    success(HttpStatus.OK, "로그아웃 성공", null),
                    errors(
                            error(ErrorCode.VALIDATION_FAILED),
                            error(ErrorCode.INTERNAL_SERVER_ERROR))),
            endpoint(ScenarioController.class, "getCategories",
                    success(HttpStatus.OK, "카테고리 목록 조회 성공", objectMap(
                            "categories", List.of(objectMap("categoryId", "cafe", "name", "카페"))
                    )),
                    errors(error(ErrorCode.INTERNAL_SERVER_ERROR))),
            endpoint(ScenarioController.class, "getScenarios",
                    success(HttpStatus.OK, "시나리오 목록 조회 성공", objectMap(
                            "scenarios", List.of(objectMap(
                                    "scenarioId", "cafe_iced_americano",
                                    "categoryId", "cafe",
                                    "title", "아이스 아메리카노 주문하기",
                                    "difficulty", "쉬움",
                                    "situationDescription", "카페에서 원하는 음료를 주문해야 합니다.",
                                    "successGoal", "아이스 아메리카노 주문에 성공하세요.",
                                    "thumbnailUrl", null))
                    )),
                    errors(error(ErrorCode.CATEGORY_NOT_FOUND), error(ErrorCode.INTERNAL_SERVER_ERROR))),
            endpoint(PracticeSessionController.class, "startSession",
                    success(HttpStatus.CREATED, "세션 시작 성공", objectMap(
                            "sessionId", "550e8400-e29b-41d4-a716-446655440000",
                            "scenarioId", "cafe_iced_americano",
                            "status", "IN_PROGRESS",
                            "babsaeText", "Hi! What would you like to order?",
                            "babsaeTtsUrl", null,
                            "followUpCount", 0,
                            "maxFollowUpCount", 5,
                            "startedAt", "2026-05-08T00:00:00"
                    )),
                    errors(
                            error(ErrorCode.AUTH_REQUIRED),
                            error(ErrorCode.VALIDATION_FAILED),
                            error(ErrorCode.SCENARIO_NOT_FOUND),
                            error(ErrorCode.INTERNAL_SERVER_ERROR))),
            endpoint(PracticeSessionController.class, "getSession",
                    success(HttpStatus.OK, "세션 상태 조회 성공", objectMap(
                            "sessionId", "550e8400-e29b-41d4-a716-446655440000",
                            "scenarioId", "cafe_iced_americano",
                            "status", "IN_PROGRESS",
                            "babsaeText", "What size would you like?",
                            "babsaeTtsUrl", null,
                            "followUpCount", 1,
                            "maxFollowUpCount", 5,
                            "micReadyLatencyMs", 1240,
                            "turns", List.of(objectMap(
                                    "turnId", 1,
                                    "turnIndex", 1,
                                    "questionText", "Hi! What would you like to order?",
                                    "userTranscript", "I want iced americano",
                                    "speechStartedAfterMs", 2100,
                                    "recordingDurationMs", 3600,
                                    "createdAt", "2026-05-08T00:00:10"))
                    )),
                    errors(
                            error(ErrorCode.AUTH_REQUIRED),
                            error(ErrorCode.SESSION_ACCESS_DENIED),
                            error(ErrorCode.SESSION_NOT_FOUND),
                            error(ErrorCode.INTERNAL_SERVER_ERROR))),
            endpoint(PracticeSessionController.class, "recordMicReady",
                    success(HttpStatus.OK, "마이크 준비 지연 기록 성공", objectMap(
                            "sessionId", "550e8400-e29b-41d4-a716-446655440000",
                            "micReadyLatencyMs", 1240
                    )),
                    errors(
                            error(ErrorCode.AUTH_REQUIRED),
                            error(ErrorCode.SESSION_ACCESS_DENIED),
                            error(ErrorCode.SESSION_NOT_FOUND),
                            error(ErrorCode.SESSION_ALREADY_ENDED),
                            error(ErrorCode.INTERNAL_SERVER_ERROR))),
            endpoint(PracticeSessionController.class, "submitTurn",
                    success(HttpStatus.OK, "턴 음성 제출 성공", objectMap(
                            "sessionId", "550e8400-e29b-41d4-a716-446655440000",
                            "turnId", 1,
                            "turnIndex", 1,
                            "transcript", "I want iced americano",
                            "sttConfidence", 0.86,
                            "status", "IN_PROGRESS",
                            "babsaeText", "What size would you like?",
                            "babsaeTtsUrl", null,
                            "followUpCount", 1,
                            "maxFollowUpCount", 5,
                            "feedbackAvailable", false
                    )),
                    errors(
                            error(ErrorCode.AUTH_REQUIRED),
                            error(ErrorCode.SESSION_ACCESS_DENIED),
                            error(ErrorCode.VALIDATION_FAILED),
                            error(ErrorCode.UNSUPPORTED_INPUT_TYPE),
                            error(ErrorCode.AUDIO_REQUIRED),
                            error(ErrorCode.AUDIO_READ_FAILED),
                            error(ErrorCode.SESSION_NOT_FOUND),
                            error(ErrorCode.SESSION_ALREADY_ENDED),
                            error(ErrorCode.AUDIO_TOO_LARGE),
                            error(ErrorCode.UNSUPPORTED_AUDIO_TYPE),
                            error(ErrorCode.AI_STT_FAILED),
                            error(ErrorCode.AI_RESPONSE_INVALID),
                            error(ErrorCode.INTERNAL_SERVER_ERROR))),
            endpoint(PracticeSessionController.class, "exitSession",
                    success(HttpStatus.OK, "세션 중도 종료 성공", objectMap(
                            "sessionId", "550e8400-e29b-41d4-a716-446655440000",
                            "status", "ABANDONED",
                            "endedAt", "2026-05-08T00:03:00"
                    )),
                    errors(
                            error(ErrorCode.AUTH_REQUIRED),
                            error(ErrorCode.SESSION_ACCESS_DENIED),
                            error(ErrorCode.VALIDATION_FAILED),
                            error(ErrorCode.SESSION_NOT_FOUND),
                            error(ErrorCode.SESSION_ALREADY_ENDED),
                            error(ErrorCode.INTERNAL_SERVER_ERROR))),
            endpoint(FeedbackController.class, "getFeedback",
                    success(HttpStatus.OK, "세션 피드백 조회 성공", objectMap(
                            "sessionId", "550e8400-e29b-41d4-a716-446655440000",
                            "scenarioResult", "SUCCESS",
                            "totalUnderstoodScore", 85,
                            "summary", "주문 의도가 명확했고 핵심 표현을 잘 전달했습니다.",
                            "turnFeedback", List.of(objectMap(
                                    "turnId", 1,
                                    "turnIndex", 1,
                                    "questionText", "Hi! What would you like to order?",
                                    "userTranscript", "I want iced americano",
                                    "speechStartedAfterMs", 2100,
                                    "speechStartedAfterSeconds", 2.1,
                                    "understoodScore", 80,
                                    "heardAs", "I want iced americano",
                                    "betterExpression", "I'd like an iced Americano, please.",
                                    "scoreDelta", 10,
                                    "improvedUnderstoodScore", 90,
                                    "reason", "정중한 주문 표현을 쓰면 더 자연스럽습니다."))
                    )),
                    errors(
                            error(ErrorCode.AUTH_REQUIRED),
                            error(ErrorCode.SESSION_ACCESS_DENIED),
                            error(ErrorCode.SESSION_IN_PROGRESS),
                            error(ErrorCode.SESSION_NOT_FOUND),
                            error(ErrorCode.FEEDBACK_NOT_FOUND),
                            error(ErrorCode.INTERNAL_SERVER_ERROR)))
    );

    @Bean
    public OperationCustomizer apiResponseExamplesOperationCustomizer() {
        return (operation, handlerMethod) -> {
            EndpointDoc endpointDoc = ENDPOINT_DOCS.get(key(handlerMethod));
            if (endpointDoc == null) {
                return operation;
            }

            ApiResponses responses = new ApiResponses();
            responses.addApiResponse(
                    String.valueOf(endpointDoc.success().status().value()),
                    jsonResponse(endpointDoc.success().description(), "SUCCESS", endpointDoc.success().description(), successBody(endpointDoc.success().data())));

            endpointDoc.errors().stream()
                    .collect(Collectors.groupingBy(error -> error.errorCode().getStatus(), LinkedHashMap::new, Collectors.toList()))
                    .forEach((status, errors) -> responses.addApiResponse(String.valueOf(status.value()), errorResponse(errors)));
            operation.setResponses(responses);
            return operation;
        };
    }

    private static ApiResponse errorResponse(List<ErrorDoc> errors) {
        MediaType mediaType = new MediaType().schema(responseSchema());
        for (ErrorDoc error : errors) {
            ErrorCode errorCode = error.errorCode();
            mediaType.addExamples(
                    errorCode.name(),
                    new Example()
                            .summary(error.summary())
                            .value(errorBody(errorCode)));
        }
        return new ApiResponse()
                .description(errors.stream().map(ErrorDoc::summary).collect(Collectors.joining(" / ")))
                .content(new Content().addMediaType(APPLICATION_JSON, mediaType));
    }

    private static ApiResponse jsonResponse(String description, String exampleName, String exampleSummary, Object exampleValue) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(APPLICATION_JSON, new MediaType()
                        .schema(responseSchema())
                        .addExamples(exampleName, new Example().summary(exampleSummary).value(exampleValue))));
    }

    private static Schema<?> responseSchema() {
        return new ObjectSchema()
                .addProperty("success", new BooleanSchema().description("요청 처리 성공 여부"))
                .addProperty("data", new Schema<>().description("성공 응답 데이터. 실패 시 null입니다.").nullable(true))
                .addProperty("error", new Schema<>().description("실패 오류 정보. 성공 시 null입니다.").nullable(true));
    }

    private static Map.Entry<String, EndpointDoc> endpoint(Class<?> controllerType, String methodName, SuccessDoc success, List<ErrorDoc> errors) {
        return Map.entry(key(controllerType, methodName), new EndpointDoc(success, errors));
    }

    private static SuccessDoc success(HttpStatus status, String description, Object data) {
        return new SuccessDoc(status, description, data);
    }

    private static List<ErrorDoc> errors(ErrorDoc... errors) {
        return List.of(errors);
    }

    private static ErrorDoc error(ErrorCode errorCode) {
        return new ErrorDoc(errorCode, errorCode.getMessage());
    }

    private static Map<String, Object> successBody(Object data) {
        return objectMap("success", true, "data", data, "error", null);
    }

    private static Map<String, Object> errorBody(ErrorCode errorCode) {
        return objectMap(
                "success", false,
                "data", null,
                "error", objectMap("code", errorCode.name(), "message", errorCode.getMessage())
        );
    }

    private static String key(HandlerMethod handlerMethod) {
        return key(handlerMethod.getBeanType(), handlerMethod.getMethod().getName());
    }

    private static String key(Class<?> controllerType, String methodName) {
        return controllerType.getName() + "#" + methodName;
    }

    private static Map<String, Object> objectMap(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues length must be even");
        }

        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            map.put((String) keyValues[index], keyValues[index + 1]);
        }
        return map;
    }

    private record EndpointDoc(SuccessDoc success, List<ErrorDoc> errors) {
    }

    private record SuccessDoc(HttpStatus status, String description, Object data) {
    }

    private record ErrorDoc(ErrorCode errorCode, String summary) {
    }
}

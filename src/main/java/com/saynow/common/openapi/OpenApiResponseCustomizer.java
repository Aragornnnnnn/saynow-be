// 공통 ApiResponse 래퍼 기준으로 3차 MVP 주요 API 응답 예시를 주입하는 OpenAPI 설정
package com.saynow.common.openapi;

import com.saynow.auth.api.AuthController;
import com.saynow.common.exception.ErrorCode;
import com.saynow.feedback.api.FeedbackController;
import com.saynow.nps.api.NpsController;
import com.saynow.scenario.api.ScenarioController;
import com.saynow.session.api.SessionController;
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
                            "user", objectMap(
                                    "userId", "1",
                                    "nickname", "Ryan",
                                    "email", "ryan@example.com",
                                    "provider", "GOOGLE",
                                    "newUser", true)
                    )),
                    errors(error(ErrorCode.VALIDATION_FAILED), error(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER), error(ErrorCode.INTERNAL_SERVER_ERROR))),
            endpoint(AuthController.class, "refresh",
                    success(HttpStatus.OK, "토큰 재발급 성공", objectMap(
                            "tokenType", "Bearer",
                            "accessToken", "new-saynow-access-token",
                            "accessTokenExpiresIn", 1800,
                            "refreshToken", "new-saynow-refresh-token",
                            "refreshTokenExpiresIn", 1209600
                    )),
                    errors(error(ErrorCode.VALIDATION_FAILED), error(ErrorCode.REFRESH_TOKEN_INVALID), error(ErrorCode.INTERNAL_SERVER_ERROR))),
            endpoint(AuthController.class, "logout",
                    success(HttpStatus.OK, "로그아웃 성공", null),
                    errors(error(ErrorCode.VALIDATION_FAILED), error(ErrorCode.INTERNAL_SERVER_ERROR))),
            endpoint(ScenarioController.class, "getScenarios",
                    success(HttpStatus.OK, "시나리오 전체 조회 성공", objectMap(
                            "categories", List.of(
                                    objectMap(
                                            "categoryId", 1,
                                            "categoryName", "Free Talk",
                                            "categoryLocked", false,
                                            "categoryLockReason", null,
                                            "scenarios", List.of(objectMap(
                                                    "scenarioId", 1,
                                                    "displayOrder", 1,
                                                    "scenarioTitle", "음식 취향 이야기하기",
                                                    "briefing", "좋아하는 음식과 최근 먹었던 음식에 대해 이야기합니다.",
                                                    "conversationGoal", "음식 취향과 경험을 영어로 자연스럽게 설명할 수 있다.",
                                                    "completed", false,
                                                    "locked", false,
                                                    "lockReason", null,
                                                    "firstQuestionPreview", objectMap(
                                                            "questionId", 100,
                                                            "aiQuestion", "What is your favorite food? Why do you like it?",
                                                            "translatedQuestion", "가장 좋아하는 음식이 뭐예요? 왜 좋아하나요?")))),
                                    objectMap(
                                            "categoryId", 2,
                                            "categoryName", "Airport",
                                            "categoryLocked", true,
                                            "categoryLockReason", "COMING_SOON",
                                            "scenarios", List.of()))
                    )),
                    errors(error(ErrorCode.AUTH_REQUIRED), error(ErrorCode.INTERNAL_SERVER_ERROR))),
            endpoint(SessionController.class, "startSession",
                    success(HttpStatus.CREATED, "시나리오 세션 시작 성공", objectMap(
                            "sessionId", 12,
                            "scenarioId", 1,
                            "totalQuestionCount", 4,
                            "currentTurn", objectMap(
                                    "turnId", 101,
                                    "sequence", 1,
                                    "aiQuestion", "What is your favorite food? Why do you like it?",
                                    "translatedQuestion", "가장 좋아하는 음식이 뭐예요? 왜 좋아하나요?"),
                            "progress", objectMap(
                                    "currentSequence", 1,
                                    "totalQuestionCount", 4,
                                    "completed", false)
                    )),
                    errors(error(ErrorCode.AUTH_REQUIRED), error(ErrorCode.SCENARIO_NOT_FOUND), error(ErrorCode.SCENARIO_LOCKED), error(ErrorCode.CATEGORY_LOCKED))),
            endpoint(SessionController.class, "submitUtterance",
                    success(HttpStatus.OK, "세션 사용자 발화 제출 성공", objectMap(
                            "submittedTurn", objectMap(
                                    "turnId", 101,
                                    "sequence", 1,
                                    "turnFeedbackStatus", "PREPARING"),
                            "nextTurn", objectMap(
                                    "turnId", 102,
                                    "sequence", 2,
                                    "aiQuestion", "That sounds tasty. Do you cook often?",
                                    "translatedQuestion", "맛있겠네요. 요리는 자주 하나요?"),
                            "progress", objectMap(
                                    "currentSequence", 2,
                                    "totalQuestionCount", 4,
                                    "completed", false)
                    )),
                    errors(error(ErrorCode.SESSION_NOT_FOUND), error(ErrorCode.SESSION_ALREADY_COMPLETED), error(ErrorCode.INVALID_REQUEST), error(ErrorCode.AUTH_REQUIRED), error(ErrorCode.FORBIDDEN), error(ErrorCode.AI_RESPONSE_INVALID))),
            endpoint(SessionController.class, "abandonSession",
                    success(HttpStatus.OK, "세션 중도 종료 성공", null),
                    errors(error(ErrorCode.AUTH_REQUIRED), error(ErrorCode.FORBIDDEN), error(ErrorCode.SESSION_NOT_FOUND), error(ErrorCode.SESSION_ALREADY_COMPLETED))),
            endpoint(FeedbackController.class, "createFeedback",
                    success(HttpStatus.OK, "세션 최종 피드백 생성 성공", objectMap(
                            "sessionId", 12,
                            "nativeScore", 82,
                            "nativeLevelLabel", "유학생 수준",
                            "summary", "하고 싶은 말을 끝까지 전달하는 힘이 좋았어요.",
                            "turnFeedbacks", List.of(objectMap(
                                    "turnId", 101,
                                    "sequence", 1,
                                    "originalQuestion", "What is your favorite food? Why do you like it?",
                                    "translatedQuestion", "가장 좋아하는 음식이 뭐예요? 왜 좋아하나요?",
                                    "userUtterance", "I like pizza because it is spicy.",
                                    "feedbackType", "GOOD",
                                    "koreanAnalogy", "한국어로 비유하자면 담백하게 이유를 붙인 말처럼 들려요.",
                                    "feedbackDetail", "좋아하는 음식과 이유를 한 문장 안에서 분명하게 연결했기 때문이에요.",
                                    "betterExpression", null))
                    )),
                    errors(error(ErrorCode.AUTH_REQUIRED), error(ErrorCode.FORBIDDEN), error(ErrorCode.SESSION_NOT_FOUND), error(ErrorCode.SESSION_NOT_COMPLETED), error(ErrorCode.FEEDBACK_NOT_READY), error(ErrorCode.FEEDBACK_GENERATION_FAILED))),
            endpoint(NpsController.class, "submitNps",
                    success(HttpStatus.CREATED, "세션 NPS 평가 제출 성공", null),
                    errors(error(ErrorCode.AUTH_REQUIRED), error(ErrorCode.FORBIDDEN), error(ErrorCode.SESSION_NOT_FOUND), error(ErrorCode.SESSION_IN_PROGRESS), error(ErrorCode.INVALID_REQUEST), error(ErrorCode.NPS_ALREADY_SUBMITTED)))
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

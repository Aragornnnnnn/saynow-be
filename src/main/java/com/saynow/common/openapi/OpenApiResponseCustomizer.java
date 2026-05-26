// 공통 ApiResponse 래퍼 기준으로 주요 API 응답 예시를 주입하는 OpenAPI 설정
package com.saynow.common.openapi;

import com.saynow.auth.api.AuthController;
import com.saynow.common.exception.ErrorCode;
import com.saynow.feedback.api.FeedbackController;
import com.saynow.session.api.SessionController;
import com.saynow.scenario.api.ScenarioController;
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
                                            "categoryName", "Cafe",
                                            "categoryLocked", true,
                                            "categoryLockReason", "COMING_SOON",
                                            "scenarios", List.of()),
                                    objectMap(
                                            "categoryId", 2,
                                            "categoryName", "Airport",
                                            "categoryLocked", false,
                                            "categoryLockReason", null,
                                            "scenarios", List.of(objectMap(
                                                    "scenarioId", 4,
                                                    "displayOrder", 1,
                                                    "scenarioTitle", "공항에서 입국심사 받기",
                                                    "scenarioGoal", "입국 목적과 체류 정보를 설명하고 입국심사를 통과할 수 있다.",
                                                    "scenarioSituation", "미국 공항에 도착해 입국심사를 받는 상황입니다. 심사관의 질문에 여행 계획을 차분히 설명해야 합니다.",
                                                    "scenarioEmoji", "🛂",
                                                    "cleared", false,
                                                    "locked", false,
                                                    "lockReason", null)))
                                    )
                    )),
                    errors(error(ErrorCode.AUTH_REQUIRED), error(ErrorCode.INTERNAL_SERVER_ERROR))),
            endpoint(SessionController.class, "startSession",
                    success(HttpStatus.CREATED, "시나리오 세션 시작 성공", objectMap(
                            "sessionId", 12,
                            "originalQuestion", "Hi, what's the purpose of your visit?",
                            "translatedQuestion", "안녕하세요. 방문 목적이 어떻게 되시나요?",
                            "remainingHearts", 3,
                            "feedbackAvailable", false
                    )),
                    errors(error(ErrorCode.AUTH_REQUIRED), error(ErrorCode.SCENARIO_NOT_FOUND), error(ErrorCode.SCENARIO_LOCKED), error(ErrorCode.CATEGORY_LOCKED))),
            endpoint(SessionController.class, "submitUtterance",
                    success(HttpStatus.OK, "세션 사용자 발화 제출 성공", objectMap(
                            "sessionId", 12,
                            "originalQuestion", "What size would you like?",
                            "translatedQuestion", "어떤 사이즈로 드릴까요?",
                            "remainingHearts", 3,
                            "feedbackAvailable", false,
                            "heartDeducted", false,
                            "turnClassification", "ANSWER"
                    )),
                    errors(error(ErrorCode.SESSION_NOT_FOUND), error(ErrorCode.SESSION_ALREADY_COMPLETED), error(ErrorCode.INVALID_REQUEST), error(ErrorCode.AUTH_REQUIRED), error(ErrorCode.FORBIDDEN), error(ErrorCode.AI_RESPONSE_INVALID))),
            endpoint(SessionController.class, "generateGuideAnswer",
                    success(HttpStatus.OK, "세션 중 영어 학습 가이드 답변 생성 성공", objectMap(
                            "answer", "would는 공손한 요청이나 가정 느낌을 줄 때 써요."
                    )),
                    errors(error(ErrorCode.AUTH_REQUIRED), error(ErrorCode.FORBIDDEN), error(ErrorCode.SESSION_NOT_FOUND), error(ErrorCode.SESSION_ALREADY_COMPLETED), error(ErrorCode.INVALID_REQUEST), error(ErrorCode.AI_GENERATION_FAILED))),
            endpoint(SessionController.class, "getSessionResult",
                    success(HttpStatus.OK, "세션 시나리오 결과 조회 성공", objectMap(
                            "scenarioResult", "SUCCESS"
                    )),
                    errors(error(ErrorCode.AUTH_REQUIRED), error(ErrorCode.FORBIDDEN), error(ErrorCode.SESSION_NOT_FOUND), error(ErrorCode.SESSION_NOT_COMPLETABLE))),
            endpoint(SessionController.class, "deleteSession",
                    success(HttpStatus.OK, "세션 중도 종료 성공", null),
                    errors(error(ErrorCode.AUTH_REQUIRED), error(ErrorCode.FORBIDDEN), error(ErrorCode.SESSION_NOT_FOUND), error(ErrorCode.SESSION_ALREADY_COMPLETED))),
            endpoint(FeedbackController.class, "createFeedback",
                    success(HttpStatus.OK, "세션 최종 피드백 생성 성공", objectMap(
                            "sessionId", 12,
                            "cleared", true,
                            "comprehensionScore", 82,
                            "feedbackSummary", "전체적으로 의도는 잘 전달됐지만 주문 표현이 조금 짧게 들립니다.",
                            "remainingHearts", 1,
                            "turnFeedbacks", List.of(objectMap(
                                    "turnId", 101,
                                    "sequence", 1,
                                    "originalQuestion", "What would you like to order?",
                                    "translatedQuestion", "무엇을 주문하시겠어요?",
                                    "userUtterance", "I want iced americano.",
                                    "feedbackRequired", true,
                                    "nativeUnderstanding", "아이스 아메리카노를 주문하고 싶다는 의미로 이해됩니다.",
                                    "nativeLanguageInterpretation", "조금 짧고 문법적으로 어색하게 들립니다.",
                                    "betterExpression", "I'd like an iced Americano, please."))
                    )),
                    errors(error(ErrorCode.AUTH_REQUIRED), error(ErrorCode.FORBIDDEN), error(ErrorCode.SESSION_NOT_FOUND), error(ErrorCode.SESSION_NOT_COMPLETABLE), error(ErrorCode.SESSION_ALREADY_COMPLETED), error(ErrorCode.FEEDBACK_GENERATION_FAILED)))
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

// 공통 ApiResponse 래퍼 기준으로 3차 MVP 주요 API 응답 예시를 주입하는 OpenAPI 설정
package com.saynow.common.openapi;

import com.saynow.appversion.api.AppVersionController;
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
            endpoint(AppVersionController.class, "checkAppVersion",
                    success(HttpStatus.OK, "앱 버전 업데이트 확인 성공", objectMap(
                            "updateType", "SOFT",
                            "latestVersionName", "1.4.0",
                            "latestBuildNumber", 18,
                            "minimumSupportedBuildNumber", 15,
                            "reason", "새로운 대화 품질 개선이 포함되어 있습니다.",
                            "releasedAt", "2026-06-09T12:00:00"
                    )),
                    errors(error(ErrorCode.VALIDATION_FAILED), error(ErrorCode.INTERNAL_SERVER_ERROR))),
            endpoint(ScenarioController.class, "getScenarios",
                    success(HttpStatus.OK, "시나리오 전체 조회 성공", objectMap(
                            "categories", List.of(
                                    objectMap(
                                            "categoryId", 1,
                                            "categoryName", "룸메이트",
                                            "categoryLocked", false,
                                            "categoryLockReason", null,
                                            "scenarios", List.of(objectMap(
                                                    "scenarioId", 1,
                                                    "displayOrder", 1,
                                                    "scenarioTitle", "입주 첫날 — charlie와 첫 만남",
                                                    "briefing", "입주 첫날 룸메이트 charlie와 서로를 소개하고, 공동생활 방식과 식사, 파티 초대에 대해 이야기합니다.",
                                                    "conversationGoal", "룸메이트와 첫 만남에서 자기소개, 공동생활 방식, 음식 취향, 초대 거절을 부드럽게 말한다.",
                                                    "completed", false,
                                                    "locked", false,
                                                    "lockReason", null,
                                                    "firstQuestionPreview", objectMap(
                                                            "questionId", 100,
                                                            "aiQuestion", "Hey, you must be my roommate! I'm charlie. Okay, tell me everything — what are you studying, what are you into?",
                                                            "translatedQuestion", "야 너 내 룸메지! 난 charlie야. 자, 다 얘기해봐 — 뭐 전공하고 뭐 좋아해?")))),
                                    objectMap(
                                            "categoryId", 2,
                                            "categoryName", "수업",
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
                                    "aiQuestion", "Hey, you must be my roommate! I'm charlie. Okay, tell me everything — what are you studying, what are you into?",
                                    "translatedQuestion", "야 너 내 룸메지! 난 charlie야. 자, 다 얘기해봐 — 뭐 전공하고 뭐 좋아해?"),
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
                                    "turnFeedbackStatus", "PREPARING",
                                    "innerThought", "첫 만남인데 전공과 취미를 자연스럽게 알려줘서 말 걸기 편하겠다.",
                                    "innerThoughtType", "GOOD"),
                            "nextTurn", objectMap(
                                    "turnId", 102,
                                    "sequence", 2,
                                    "aiQuestion", "Nice. What made you decide to come all the way here?",
                                    "translatedQuestion", "좋아. 너는 어쩌다 여기까지 오게 된 거야?"),
                            "progress", objectMap(
                                    "currentSequence", 2,
                                    "totalQuestionCount", 4,
                                    "completed", false)
                    ), Map.of(
                            "COMPLETED", objectMap(
                                    "submittedTurn", objectMap(
                                            "turnId", 104,
                                            "sequence", 4,
                                            "turnFeedbackStatus", "PREPARING",
                                            "innerThought", "못 먹는 음식을 부드럽게 말해줘서 저녁 메뉴를 맞추기 쉽겠다.",
                                            "innerThoughtType", "GOOD"),
                                    "nextTurn", objectMap(
                                            "turnId", 105,
                                            "sequence", 5,
                                            "aiQuestion", "Got it. I'll avoid fish, and we can share dinner tonight.",
                                            "translatedQuestion", "알겠어. 생선은 피해서 오늘 저녁 같이 먹자."),
                                    "progress", objectMap(
                                            "currentSequence", 5,
                                            "totalQuestionCount", 4,
                                            "completed", true)))),
                    errors(error(ErrorCode.SESSION_NOT_FOUND), error(ErrorCode.SESSION_ALREADY_COMPLETED), error(ErrorCode.INVALID_REQUEST), error(ErrorCode.AUTH_REQUIRED), error(ErrorCode.FORBIDDEN), error(ErrorCode.AI_GENERATION_FAILED))),
            endpoint(SessionController.class, "abandonSession",
                    success(HttpStatus.OK, "세션 중도 종료 성공", null),
                    errors(error(ErrorCode.AUTH_REQUIRED), error(ErrorCode.FORBIDDEN), error(ErrorCode.SESSION_NOT_FOUND), error(ErrorCode.SESSION_ALREADY_COMPLETED))),
            endpoint(FeedbackController.class, "createFeedback",
                    success(HttpStatus.OK, "세션 최종 피드백 생성 성공", objectMap(
                            "sessionId", 12,
                            "nativeScore", 82,
                            "highlightMessage", "한국인의 40%가 헷갈리는 간접의문문 어순을 피해 간 사람이에요.",
                            "turnFeedbacks", List.of(
                                    objectMap(
                                            "turnId", 101,
                                            "sequence", 1,
                                            "originalQuestion", "Hey, you must be my roommate! I'm charlie. Okay, tell me everything — what are you studying, what are you into?",
                                            "translatedQuestion", "야 너 내 룸메지! 난 charlie야. 자, 다 얘기해봐 — 뭐 전공하고 뭐 좋아해?",
                                            "userUtterance", "I study business, and I like playing games after class.",
                                            "feedbackType", "GOOD",
                                            "koreanAnalogy", "한국어로 비유하자면 담백하게 이유를 붙인 말처럼 들려요.",
                                            "positiveFeedback", null,
                                            "feedbackDetail", "전공과 취미를 한 문장 안에서 분명하게 연결했기 때문이에요.",
                                            "correctionExpression", null,
                                            "correctionReason", null,
                                            "benchmarkMessage", "한국인의 35%가 틀리는 표현인데 정확히 맞췄어요."),
                                    objectMap(
                                            "turnId", 102,
                                            "sequence", 2,
                                            "originalQuestion", "Wait, I'm so curious — what made you decide to come all the way here? Like, why this country?",
                                            "translatedQuestion", "잠깐, 나 완전 궁금해 — 너 어쩌다 여기까지 오게 된 거야? 그러니까, 왜 이 나라야?",
                                            "userUtterance", "Because English.",
                                            "feedbackType", "NEEDS_IMPROVEMENT",
                                            "koreanAnalogy", "한국어로 비유하자면 조금 단어만 놓고 말한 느낌이에요.",
                                            "positiveFeedback", "어려운 표현에 도전한 점은 좋아요.",
                                            "feedbackDetail", null,
                                            "correctionExpression", "I came here because I wanted to improve my English.",
                                            "correctionReason", "because 뒤에 완전한 문장을 붙이면 이유가 더 자연스럽게 전달돼요.",
                                            "benchmarkMessage", null))
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
                    jsonResponse(
                            endpointDoc.success().description(),
                            "SUCCESS",
                            endpointDoc.success().description(),
                            successBody(endpointDoc.success().data()),
                            endpointDoc.success().additionalExamples()));

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

    private static ApiResponse jsonResponse(
            String description,
            String exampleName,
            String exampleSummary,
            Object exampleValue,
            Map<String, Object> additionalExamples
    ) {
        MediaType mediaType = new MediaType()
                .schema(responseSchema())
                .addExamples(exampleName, new Example().summary(exampleSummary).value(exampleValue));
        additionalExamples.forEach((name, value) -> mediaType.addExamples(
                name,
                new Example().summary(name).value(successBody(value))));
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(APPLICATION_JSON, mediaType));
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
        return new SuccessDoc(status, description, data, Map.of());
    }

    private static SuccessDoc success(HttpStatus status, String description, Object data, Map<String, Object> additionalExamples) {
        return new SuccessDoc(status, description, data, additionalExamples);
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

    private record SuccessDoc(HttpStatus status, String description, Object data, Map<String, Object> additionalExamples) {
    }

    private record ErrorDoc(ErrorCode errorCode, String summary) {
    }
}

// 2차 MVP 시나리오 세션 시작, 발화 제출, 중도 종료를 처리하는 서비스
package com.saynow.session.application;

import com.saynow.auth.domain.User;
import com.saynow.auth.infrastructure.UserRepository;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.session.api.dto.SessionResultResponse;
import com.saynow.session.api.dto.SessionStartResponse;
import com.saynow.session.api.dto.UserUtteranceRequest;
import com.saynow.session.api.dto.UserUtteranceResponse;
import com.saynow.session.domain.Session;
import com.saynow.session.domain.SessionTurn;
import com.saynow.session.domain.SessionSlotStatus;
import com.saynow.session.domain.SessionStatus;
import com.saynow.session.infrastructure.SessionRepository;
import com.saynow.session.infrastructure.SessionTurnRepository;
import com.saynow.session.infrastructure.SessionSlotStatusRepository;
import com.saynow.session.infrastructure.ai.AiConversationClient;
import com.saynow.session.infrastructure.ai.AiFilledSlot;
import com.saynow.session.infrastructure.ai.AiNextQuestionRequest;
import com.saynow.session.infrastructure.ai.AiNextQuestionResponse;
import com.saynow.session.infrastructure.ai.AiSlotStatus;
import com.saynow.session.infrastructure.ai.TurnClassification;
import com.saynow.scenario.application.ScenarioService;
import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.UserScenarioProgress;
import com.saynow.scenario.infrastructure.ScenarioRepository;
import com.saynow.scenario.infrastructure.ScenarioSlotRepository;
import com.saynow.scenario.infrastructure.UserScenarioProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final String OPEN_CATEGORY_NAME = "Airport";

    private final ScenarioRepository scenarioRepository;
    private final ScenarioSlotRepository scenarioSlotRepository;
    private final SessionRepository sessionRepository;
    private final SessionTurnRepository turnRepository;
    private final SessionSlotStatusRepository slotStatusRepository;
    private final UserScenarioProgressRepository userScenarioProgressRepository;
    private final UserRepository userRepository;
    private final AiConversationClient aiConversationClient;

    @Transactional
    public SessionStartResponse startSession(Long userId, Long scenarioId) {
        User user = findUser(userId);
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ApiException(ErrorCode.SCENARIO_NOT_FOUND));
        assertPlayable(user, scenario);
        userScenarioProgressRepository.findByUserAndScenario(user, scenario)
                .orElseGet(() -> userScenarioProgressRepository.save(new UserScenarioProgress(user, scenario)));

        Session session = sessionRepository.save(new Session(user, scenario));
        turnRepository.save(new SessionTurn(session, 1, scenario.getOriginalQuestion(), scenario.getTranslatedQuestion()));
        scenarioSlotRepository.findByScenarioOrderByIdAsc(scenario)
                .forEach(slot -> slotStatusRepository.save(new SessionSlotStatus(session, slot.getName())));

        return new SessionStartResponse(
                session.getId(),
                scenario.getOriginalQuestion(),
                scenario.getTranslatedQuestion(),
                session.getRemainingHearts(),
                false);
    }

    @Transactional
    public UserUtteranceResponse submitUtterance(Long userId, Long sessionId, UserUtteranceRequest request) {
        Session session = findOwnedSession(userId, sessionId);
        assertInProgress(session);
        validateUserUtterance(request);

        SessionTurn currentTurn = turnRepository.findBySessionAndUserUtteranceIsNullOrderBySequenceAsc(session)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_COMPLETABLE));
        currentTurn.submitUserUtterance(request.userUtterance().trim());

        List<SessionSlotStatus> slotStatuses = slotStatusRepository.findBySessionOrderByIdAsc(session);
        AiNextQuestionResponse aiResponse = aiConversationClient.generateNextQuestion(new AiNextQuestionRequest(
                currentTurn.getAiQuestion(),
                currentTurn.getUserUtterance(),
                session.getScenario().getTitle(),
                session.getScenario().getSituation(),
                session.getScenario().getGoal(),
                slotStatuses.stream()
                        .map(slot -> new AiSlotStatus(slot.getSlotName(), slot.isFulfilled()))
                        .toList()));
        validateNextQuestionResponse(aiResponse);

        boolean heartDeducted = shouldDeductHeart(session, aiResponse);
        applyFilledSlots(slotStatuses, aiResponse.filledSlots());
        if (heartDeducted) {
            session.decreaseHeart();
        }

        if (allFulfilled(slotStatuses)) {
            session.complete(SessionStatus.SUCCESS, LocalDateTime.now());
            markScenarioCleared(session);
            return completedResponse(session, heartDeducted, aiResponse.turnClassification());
        }
        if (session.getRemainingHearts() <= 0) {
            session.complete(SessionStatus.FAILURE, LocalDateTime.now());
            return completedResponse(session, heartDeducted, aiResponse.turnClassification());
        }
        if (aiResponse.nextQuestion() == null || aiResponse.nextQuestion().isBlank()
                || aiResponse.translatedQuestion() == null || aiResponse.translatedQuestion().isBlank()) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }

        turnRepository.save(new SessionTurn(
                session,
                currentTurn.getSequence() + 1,
                aiResponse.nextQuestion(),
                aiResponse.translatedQuestion()));
        return new UserUtteranceResponse(
                session.getId(),
                aiResponse.nextQuestion(),
                aiResponse.translatedQuestion(),
                session.getRemainingHearts(),
                false,
                heartDeducted,
                aiResponse.turnClassification());
    }

    @Transactional(readOnly = true)
    public SessionResultResponse getSessionResult(Long userId, Long sessionId) {
        Session session = findOwnedSession(userId, sessionId);
        if (session.getStatus() != SessionStatus.SUCCESS && session.getStatus() != SessionStatus.FAILURE) {
            throw new ApiException(ErrorCode.SESSION_NOT_COMPLETABLE);
        }
        return new SessionResultResponse(session.getStatus().name());
    }

    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        Session session = findOwnedSession(userId, sessionId);
        if (!session.isInProgress()) {
            throw new ApiException(ErrorCode.SESSION_ALREADY_COMPLETED);
        }
        sessionRepository.delete(session);
    }

    private User findUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_REQUIRED));
    }

    private Session findOwnedSession(Long userId, Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
        if (!session.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return session;
    }

    private void assertInProgress(Session session) {
        if (!session.isInProgress()) {
            throw new ApiException(ErrorCode.SESSION_ALREADY_COMPLETED);
        }
    }

    private void validateUserUtterance(UserUtteranceRequest request) {
        if (request == null || request.userUtterance() == null || request.userUtterance().isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void markScenarioCleared(Session session) {
        UserScenarioProgress progress = userScenarioProgressRepository.findByUserAndScenario(session.getUser(), session.getScenario())
                .orElseGet(() -> userScenarioProgressRepository.save(new UserScenarioProgress(session.getUser(), session.getScenario())));
        progress.markCleared();
    }

    private void assertPlayable(User user, Scenario scenario) {
        if (!OPEN_CATEGORY_NAME.equals(scenario.getCategory().getName())) {
            throw new ApiException(ErrorCode.CATEGORY_LOCKED);
        }

        List<Scenario> scenarios = scenarioRepository.findByCategoryOrderByDisplayOrderAsc(scenario.getCategory());
        Map<Long, Boolean> clearedByScenarioId = userScenarioProgressRepository.findByUserIdAndScenarioIdIn(
                        user.getId(),
                        scenarios.stream().map(Scenario::getId).toList())
                .stream()
                .collect(Collectors.toMap(progress -> progress.getScenario().getId(), UserScenarioProgress::isCleared));

        boolean previousCleared = true;
        for (Scenario candidate : scenarios) {
            if (candidate.getId().equals(scenario.getId())) {
                if (!previousCleared) {
                    throw new ApiException(ErrorCode.SCENARIO_LOCKED, ScenarioService.PREVIOUS_SCENARIO_NOT_CLEARED);
                }
                return;
            }
            previousCleared = clearedByScenarioId.getOrDefault(candidate.getId(), false);
        }
    }

    private void validateNextQuestionResponse(AiNextQuestionResponse response) {
        if (response == null || response.filledSlots() == null || response.turnClassification() == null) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private boolean shouldDeductHeart(Session session, AiNextQuestionResponse response) {
        return response.turnClassification() == TurnClassification.INVALID_RESPONSE
                && session.getRemainingHearts() > 0;
    }

    private void applyFilledSlots(List<SessionSlotStatus> slotStatuses, List<AiFilledSlot> filledSlots) {
        Set<String> allowedSlotNames = slotStatuses.stream()
                .map(SessionSlotStatus::getSlotName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> filledSlotNames = filledSlots.stream()
                .map(AiFilledSlot::slotName)
                .filter(allowedSlotNames::contains)
                .collect(Collectors.toSet());

        slotStatuses.stream()
                .filter(slot -> filledSlotNames.contains(slot.getSlotName()))
                .forEach(SessionSlotStatus::fulfill);
    }

    private boolean allFulfilled(List<SessionSlotStatus> slotStatuses) {
        return slotStatuses.stream().allMatch(SessionSlotStatus::isFulfilled);
    }

    private UserUtteranceResponse completedResponse(
            Session session,
            boolean heartDeducted,
            TurnClassification turnClassification
    ) {
        return new UserUtteranceResponse(
                session.getId(),
                null,
                null,
                session.getRemainingHearts(),
                true,
                heartDeducted,
                turnClassification);
    }
}

package com.saynow.practice.infrastructure.ai;

import com.saynow.practice.domain.PromptType;
import com.saynow.practice.domain.SessionStatus;
import com.saynow.scenario.domain.ScenarioSlot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class LocalAiPracticeClient {

    public AiTurnEvaluationResult evaluateTurn(AiTurnEvaluationRequest request) {
        String transcript = transcribe(request.audioContent());
        BigDecimal sttConfidence = confidenceFor(transcript);
        List<AiFilledSlot> filledSlots = detectFilledSlots(transcript, request.scenarioSlots(), request.currentFilledSlotKeys());
        Set<String> filledAfterTurn = new LinkedHashSet<>(request.currentFilledSlotKeys());
        filledSlots.stream()
                .map(AiFilledSlot::slotKey)
                .forEach(filledAfterTurn::add);

        Optional<ScenarioSlot> firstMissingRequiredSlot = request.scenarioSlots().stream()
                .filter(ScenarioSlot::isRequired)
                .filter(slot -> !filledAfterTurn.contains(slot.getSlotKey()))
                .findFirst();

        if (firstMissingRequiredSlot.isEmpty()) {
            return new AiTurnEvaluationResult(
                    transcript,
                    sttConfidence,
                    SessionStatus.SUCCESS,
                    filledSlots,
                    null,
                    new AiPrompt(PromptType.RESULT, "Scenario cleared.", null));
        }

        if (request.followUpCount() >= request.maxFollowUpCount()) {
            return new AiTurnEvaluationResult(
                    transcript,
                    sttConfidence,
                    SessionStatus.FAILURE,
                    filledSlots,
                    null,
                    new AiPrompt(PromptType.RESULT, "The scenario was not cleared in time.", null));
        }

        ScenarioSlot missingSlot = firstMissingRequiredSlot.get();
        return new AiTurnEvaluationResult(
                transcript,
                sttConfidence,
                SessionStatus.IN_PROGRESS,
                filledSlots,
                new AiPrompt(PromptType.FOLLOW_UP, promptFor(missingSlot.getSlotKey()), null),
                null);
    }

    private String transcribe(byte[] audioContent) {
        return new String(audioContent, StandardCharsets.UTF_8).trim();
    }

    private BigDecimal confidenceFor(String transcript) {
        String normalized = transcript.toLowerCase(Locale.ROOT);
        if (normalized.contains("don't know")) {
            return new BigDecimal("0.71");
        }
        if (containsAny(normalized, "small", "for here", "to go")) {
            return new BigDecimal("0.92");
        }
        return new BigDecimal("0.86");
    }

    private List<AiFilledSlot> detectFilledSlots(String transcript, List<ScenarioSlot> slots, Set<String> currentFilledSlotKeys) {
        String normalized = transcript.toLowerCase(Locale.ROOT);
        List<AiFilledSlot> filledSlots = new ArrayList<>();
        for (ScenarioSlot slot : slots) {
            if (currentFilledSlotKeys.contains(slot.getSlotKey())) {
                continue;
            }
            detectSlotValue(slot.getSlotKey(), normalized, transcript)
                    .ifPresent(value -> filledSlots.add(new AiFilledSlot(slot.getSlotKey(), value)));
        }
        return filledSlots;
    }

    private Optional<String> detectSlotValue(String slotKey, String normalized, String original) {
        return switch (slotKey) {
            case "drink" -> firstMatch(normalized, "americano", "coffee", "latte", "tea", "juice");
            case "temperature" -> {
                if (containsAny(normalized, "iced", "ice", "cold")) {
                    yield Optional.of("iced");
                }
                if (normalized.contains("hot")) {
                    yield Optional.of("hot");
                }
                yield Optional.empty();
            }
            case "size" -> firstMatch(normalized, "small", "medium", "large", "tall", "grande", "venti");
            case "for_here_or_to_go" -> {
                if (containsAny(normalized, "for here", "dine in", "here")) {
                    yield Optional.of("for here");
                }
                if (containsAny(normalized, "to go", "take out", "takeaway", "포장")) {
                    yield Optional.of("to go");
                }
                yield Optional.empty();
            }
            case "purpose" -> containsAny(normalized, "travel", "trip", "tour", "business", "vacation")
                    ? Optional.of(original)
                    : Optional.empty();
            case "duration" -> containsAny(normalized, "day", "days", "week", "weeks", "month", "months")
                    ? Optional.of(original)
                    : Optional.empty();
            case "baggage" -> containsAny(normalized, "bag", "baggage", "luggage", "suitcase")
                    ? Optional.of(original)
                    : Optional.empty();
            case "location" -> containsAny(normalized, "where", "location", "belt", "carousel")
                    ? Optional.of(original)
                    : Optional.empty();
            case "reservation_name" -> containsAny(normalized, "name", "reservation", "booked")
                    ? Optional.of(original)
                    : Optional.empty();
            case "check_in" -> containsAny(normalized, "check in", "check-in")
                    ? Optional.of("check in")
                    : Optional.empty();
            case "item" -> containsAny(normalized, "towel", "blanket", "water")
                    ? Optional.of(original)
                    : Optional.empty();
            case "quantity" -> containsAny(normalized, "one", "two", "three", "1", "2", "3", "more", "extra")
                    ? Optional.of(original)
                    : Optional.empty();
            case "recommendation" -> containsAny(normalized, "recommend", "suggest")
                    ? Optional.of(original)
                    : Optional.empty();
            case "menu_item" -> containsAny(normalized, "menu", "pasta", "steak", "salad", "burger")
                    ? Optional.of(original)
                    : Optional.empty();
            case "check_request" -> containsAny(normalized, "check", "bill", "receipt")
                    ? Optional.of(original)
                    : Optional.empty();
            case "payment_method" -> containsAny(normalized, "card", "cash", "credit")
                    ? Optional.of(original)
                    : Optional.empty();
            case "destination" -> normalized.isBlank() ? Optional.empty() : Optional.of(original);
            case "card_payment" -> containsAny(normalized, "card", "credit")
                    ? Optional.of("card")
                    : Optional.empty();
            default -> Optional.empty();
        };
    }

    private String promptFor(String slotKey) {
        return switch (slotKey) {
            case "drink" -> "What would you like to order?";
            case "temperature" -> "Would you like that hot or iced?";
            case "size" -> "What size would you like?";
            case "for_here_or_to_go" -> "Is that for here or to go?";
            case "purpose" -> "What is the purpose of your visit?";
            case "duration" -> "How long will you stay?";
            case "baggage" -> "Can you describe your baggage?";
            case "location" -> "Where did you last see it?";
            case "reservation_name" -> "May I have the name on the reservation?";
            case "check_in" -> "Would you like to check in now?";
            case "item" -> "What do you need?";
            case "quantity" -> "How many do you need?";
            case "recommendation" -> "Would you like a recommendation?";
            case "menu_item" -> "Which menu item would you like?";
            case "check_request" -> "Would you like the check?";
            case "payment_method" -> "How would you like to pay?";
            case "destination" -> "Where would you like to go?";
            case "card_payment" -> "Would you like to pay by card?";
            default -> "Could you tell me more?";
        };
    }

    private Optional<String> firstMatch(String normalized, String... candidates) {
        for (String candidate : candidates) {
            if (normalized.contains(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private boolean containsAny(String normalized, String... candidates) {
        for (String candidate : candidates) {
            if (normalized.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}

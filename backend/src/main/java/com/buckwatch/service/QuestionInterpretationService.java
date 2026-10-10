package com.buckwatch.service;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class QuestionInterpretationService {
    public static class InterpretedScenario {
        @JsonPropertyDescription("Scenario type; use move, unsupported, or unclear.") public String scenarioType;
        public String originCityText;
        public String destinationCityText;
        public String moveMonth;
        public String horizonMonths;
        public String oneTimeMovingCosts;
        public String newMonthlyTakeHomeIncome;
        public String destinationRentOverride;
        public List<String> unsupportedChanges;
        public List<String> missingFields;
        public String clarificationQuestion;
    }
    private final OpenAiScenarioClient ai;
    public QuestionInterpretationService(OpenAiScenarioClient ai) { this.ai=ai; }
    public InterpretedScenario interpret(String question, LocalDate currentDate, String timezone) {
        if(question==null || question.isBlank()) throw new ScenarioException("INVALID_QUESTION","Question is required.");
        return ai.interpret(question, currentDate, timezone);
    }
}

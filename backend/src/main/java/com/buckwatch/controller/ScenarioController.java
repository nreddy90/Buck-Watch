package com.buckwatch.controller;

import com.buckwatch.dto.MoveScenarioRequest;
import com.buckwatch.service.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ScenarioController {
    private final CityCostService cities; private final BudgetService budgets; private final ScenarioCalculationService calculations; private final QuestionInterpretationService interpreter; private final ScenarioExplanationService explanations; private final OpenAiScenarioClient ai; private final Clock clock;
    public ScenarioController(CityCostService cities, BudgetService budgets, ScenarioCalculationService calculations, QuestionInterpretationService interpreter, ScenarioExplanationService explanations, OpenAiScenarioClient ai, Clock clock) { this.cities=cities;this.budgets=budgets;this.calculations=calculations;this.interpreter=interpreter;this.explanations=explanations;this.ai=ai;this.clock=clock; }
    @GetMapping("/cities") public Map<String,Object> cities(@RequestParam(required=false) String query) { return Map.of("cities",cities.search(query)); }
    @GetMapping("/profile") public Map<String,Object> profile() { return Map.of("profile",budgets.currentBudget(),"profileType","LOCAL_DEMO"); }
    @PostMapping("/scenarios/move") public Map<String,Object> move(@RequestBody MoveScenarioRequest request) { return Map.of("results",calculations.calculate(request)); }
    @PostMapping("/chat/scenario") public Map<String,Object> chat(@RequestBody Map<String,String> body) {
        String question=body.get("question"), timezone=body.getOrDefault("timezone","America/New_York");
        QuestionInterpretationService.InterpretedScenario parsed=interpreter.interpret(question,LocalDate.now(clock.withZone(ZoneId.of(timezone))),timezone);
        if("unsupported".equalsIgnoreCase(parsed.scenarioType) || (parsed.unsupportedChanges!=null && !parsed.unsupportedChanges.isEmpty())) return Map.of("status","unsupported_scenario","message","This question includes changes the MVP cannot model: "+(parsed.unsupportedChanges==null?"unsupported scenario type":String.join(", ",parsed.unsupportedChanges)));
        List<String> missing=parsed.missingFields==null?new ArrayList<>():new ArrayList<>(parsed.missingFields);
        if(parsed.destinationCityText==null || parsed.destinationCityText.isBlank()) missing.add("destinationCity");
        if(parsed.moveMonth==null || parsed.moveMonth.isBlank()) missing.add("moveMonth");
        if(parsed.horizonMonths==null || parsed.horizonMonths.isBlank()) missing.add("horizonMonths");
        if(parsed.oneTimeMovingCosts==null || parsed.oneTimeMovingCosts.isBlank()) missing.add("movingCosts");
        if(!missing.isEmpty()) return Map.of("status","clarification_required","clarificationQuestion", parsed.clarificationQuestion==null || parsed.clarificationQuestion.isBlank()?"Please provide the destination city, move month, forecast horizon, and moving costs (or confirm zero).":parsed.clarificationQuestion,"missingFields",missing);
        try {
            if(parsed.originCityText!=null && !parsed.originCityText.isBlank() && !cities.resolve(parsed.originCityText).city().equalsIgnoreCase(budgets.currentBudget().currentCity())) return Map.of("status","unsupported_scenario","message","The stated origin does not match the configured profile city.");
            var request=new MoveScenarioRequest(parsed.destinationCityText,YearMonth.parse(parsed.moveMonth),Integer.parseInt(parsed.horizonMonths),new java.math.BigDecimal(parsed.oneTimeMovingCosts),parsed.newMonthlyTakeHomeIncome==null||parsed.newMonthlyTakeHomeIncome.isBlank()?null:new java.math.BigDecimal(parsed.newMonthlyTakeHomeIncome),parsed.destinationRentOverride==null||parsed.destinationRentOverride.isBlank()?null:new java.math.BigDecimal(parsed.destinationRentOverride),"USD");
            Map<String,Object> results=calculations.calculate(request); String explanation;
            try { explanation=explanations.explain(results); } catch(Exception e) { explanation=explanations.fallback(results); }
            return Map.of("status","completed","results",results,"explanation",explanation);
        } catch(ScenarioException e) { throw e; } catch(Exception e) { throw new ScenarioException("AI_INVALID_RESPONSE","Interpretation was invalid; no scenario was run. Please clarify or use the manual form."); }
    }
}

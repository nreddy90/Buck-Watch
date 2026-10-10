package com.buckwatch.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.math.BigDecimal;

@Service
public class ScenarioExplanationService {
    private final OpenAiScenarioClient ai;
    public ScenarioExplanationService(OpenAiScenarioClient ai) { this.ai=ai; }
    public String explain(Map<String,Object> results) { return ai.explain(results); }
    public String fallback(Map<String,Object> r) {
        BigDecimal current=total(r.get("currentExpenses")), destination=total(r.get("destinationExpenses"));
        String moving=r.get("upfrontMovingCosts")==null?"excluded until confirmed":"$"+r.get("upfrontMovingCosts");
        return "Estimated monthly expenses change from $"+current+" to $"+destination+" (a change of $"+destination.subtract(current)+"). Monthly surplus changes by $"+r.get("monthlySurplusDifference")+". Upfront moving costs: "+moving+". Projected final balance is $"+r.get("finalScenarioBalance")+". City-index adjustments are estimates; review the listed assumptions and warnings.";
    }
    @SuppressWarnings("unchecked") private BigDecimal total(Object value) { return ((Map<String,BigDecimal>)value).values().stream().reduce(BigDecimal.ZERO,BigDecimal::add); }
}

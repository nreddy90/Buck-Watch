package com.buckwatch.service;

import com.buckwatch.model.Budget;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.*;

@Service
public class BudgetService {
    private final CityCostService cities;
    private final java.time.Clock clock;
    @Value("${buckwatch.demo.enabled:true}") private boolean demoEnabled;
    @Value("${buckwatch.demo.current-city:Atlanta, GA, United States}") private String city;
    public BudgetService(CityCostService cities, java.time.Clock clock) { this.cities = cities; this.clock=clock; }
    public Budget currentBudget() {
        if (!demoEnabled) throw new ScenarioException("PROFILE_UNAVAILABLE", "No authenticated budget profile is configured.");
        String supported = cities.resolve(city).city();
        return new Budget(supported, "USD", bd("5200"), bd("1600"), bd("500"), bd("350"), bd("450"), bd("700"), bd("8500"), LocalDate.now(clock));
    }
    private BigDecimal bd(String s) { return new BigDecimal(s); }
}

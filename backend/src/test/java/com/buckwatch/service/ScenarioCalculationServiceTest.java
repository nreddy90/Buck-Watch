package com.buckwatch.service;

import com.buckwatch.dto.MoveScenarioRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import java.math.BigDecimal;
import java.time.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ScenarioCalculationServiceTest {
    private ScenarioCalculationService calculations;
    private final YearMonth start = YearMonth.of(2026,10);
    @BeforeEach void setup() throws Exception {
        CityCostService cities=new CityCostService(new DefaultResourceLoader()); cities.load();
        Clock clock=Clock.fixed(start.atDay(1).atStartOfDay(ZoneId.of("America/New_York")).toInstant(),ZoneId.of("America/New_York"));
        BudgetService budgets=new BudgetService(cities,clock);
        var enabled=BudgetService.class.getDeclaredField("demoEnabled"); enabled.setAccessible(true); enabled.setBoolean(budgets,true);
        var city=BudgetService.class.getDeclaredField("city"); city.setAccessible(true); city.set(budgets,"Atlanta, GA, United States");
        calculations=new ScenarioCalculationService(cities,budgets,clock);
    }
    private Map<String,Object> calculate(String destination, YearMonth month, Integer horizon, BigDecimal costs, BigDecimal rent) {
        return calculations.calculate(new MoveScenarioRequest(destination,month,horizon,costs,null,rent,"USD"));
    }
    @Test void sameCityKeepsEstimatedCategoriesUnchanged() {
        var result=calculate("Atlanta, GA, United States",start.plusMonths(1),3,BigDecimal.ZERO,null);
        assertEquals(result.get("currentExpenses"),result.get("destinationExpenses"));
    }
    @Test void atlantaChicagoUsesCsvIndexRatiosAndRentOverrideWins() {
        var result=calculate("Chicago, IL, United States",start.plusMonths(1),3,BigDecimal.ZERO,new BigDecimal("2000"));
        @SuppressWarnings("unchecked") var dest=(Map<String,BigDecimal>)result.get("destinationExpenses");
        assertEquals(new BigDecimal("2000.00"),dest.get("rent"));
        assertEquals(new BigDecimal("528.01"),dest.get("groceries"));
        assertEquals(new BigDecimal("376.16"),dest.get("restaurants"));
    }
    @Test void zeroMovingCostIsChargedOnlyOnceAndPreMoveMonthIsBaseline() {
        var result=calculate("Chicago, IL, United States",start.plusMonths(1),3,BigDecimal.ZERO,null);
        @SuppressWarnings("unchecked") var months=(java.util.List<Map<String,Object>>)result.get("months");
        assertEquals(months.get(0).get("baselineBalance"),months.get(0).get("scenarioBalance"));
        assertEquals(new BigDecimal("0.00"),months.get(1).get("movingCosts"));
        assertEquals(new BigDecimal("0.00"),months.get(2).get("movingCosts"));
    }
    @Test void explicitZeroIncomeIsPreserved() {
        var result=calculations.calculate(new MoveScenarioRequest("Chicago, IL, United States",start,1,BigDecimal.ZERO,BigDecimal.ZERO,null,"USD"));
        assertTrue(((BigDecimal)result.get("scenarioMonthlySurplus")).signum()<0);
    }
    @Test void negativeBalancesAreNotClamped() {
        var result=calculate("Chicago, IL, United States",start,24,new BigDecimal("100000"),null);
        assertTrue(((BigDecimal)result.get("lowestProjectedScenarioBalance")).signum()<0);
        assertFalse(((String)result.get("firstNegativeMonth")).isBlank());
    }
    @Test void ambiguousAndUnsupportedCitiesHaveUsefulErrors() {
        var ambiguous=assertThrows(ScenarioException.class,()->calculate("Portland",start,1,BigDecimal.ZERO,null));
        assertEquals("AMBIGUOUS_CITY",ambiguous.code()); assertFalse(ambiguous.choices().isEmpty());
        assertEquals("UNSUPPORTED_CITY",assertThrows(ScenarioException.class,()->calculate("Atlantis",start,1,BigDecimal.ZERO,null)).code());
    }
}

package com.buckwatch.service;

import com.buckwatch.dto.MoveScenarioRequest;
import com.buckwatch.model.Budget;
import com.buckwatch.model.CityCost;
import org.springframework.stereotype.Service;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class ScenarioCalculationService {
    private final CityCostService cities; private final BudgetService budgets; private final Clock clock;
    public ScenarioCalculationService(CityCostService cities, BudgetService budgets, Clock clock) { this.cities=cities; this.budgets=budgets; this.clock=clock; }
    public Map<String,Object> calculate(MoveScenarioRequest req) {
        if (req == null || req.moveMonth()==null) throw new ScenarioException("INVALID_REQUEST", "moveMonth is required in YYYY-MM format.");
        if (req.horizonMonths()==null || req.horizonMonths()<1 || req.horizonMonths()>24) throw new ScenarioException("INVALID_HORIZON", "Forecast horizon must be between 1 and 24 months.");
        if ((req.movingCosts()!=null && req.movingCosts().signum()<0) || (req.newMonthlyIncome()!=null && req.newMonthlyIncome().signum()<0) || (req.destinationRent()!=null && req.destinationRent().signum()<0)) throw new ScenarioException("INVALID_AMOUNT", "Amounts cannot be negative.");
        Budget b=budgets.currentBudget(); CityCost from=cities.get(b.currentCity()), to=cities.resolve(req.destinationCity());
        if (!from.city().endsWith(", United States") || !to.city().endsWith(", United States")) throw new ScenarioException("UNSUPPORTED_SCENARIO", "Moves between supported US cities only are supported.");
        if (!b.currency().equals("USD")) throw new ScenarioException("UNSUPPORTED_CURRENCY", "Only USD budgets are supported.");
        if (req.currency()!=null && !req.currency().equalsIgnoreCase("USD")) throw new ScenarioException("UNSUPPORTED_CURRENCY", "Only USD scenarios are supported.");
        YearMonth start=YearMonth.now(clock); if(req.moveMonth().isBefore(start)) throw new ScenarioException("INVALID_DATE", "Move month cannot precede the simulation start month.");
        if(req.moveMonth().isAfter(start.plusMonths(req.horizonMonths()-1L))) throw new ScenarioException("INVALID_DATE", "Move month must fall within the forecast horizon.");
        BigDecimal rent=estimate(b.rent(),to.rent(),from.rent()), groceries=estimate(b.groceries(),to.groceries(),from.groceries()), restaurants=estimate(b.restaurants(),to.restaurants(),from.restaurants()), other=estimate(b.otherLocationSensitive(),to.costOfLiving(),from.costOfLiving());
        if(req.destinationRent()!=null) rent=req.destinationRent().setScale(2,RoundingMode.HALF_UP);
        Map<String,BigDecimal> current=Map.of("rent",money(b.rent()),"groceries",money(b.groceries()),"restaurants",money(b.restaurants()),"otherLocationSensitive",money(b.otherLocationSensitive()),"fixed",money(b.fixedExpenses()));
        Map<String,BigDecimal> destination=Map.of("rent",rent,"groceries",groceries,"restaurants",restaurants,"otherLocationSensitive",other,"fixed",money(b.fixedExpenses()));
        BigDecimal currentExpense=current.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add), destinationExpense=destination.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal income=money(b.monthlyIncome()), newIncome=req.newMonthlyIncome()==null?income:money(req.newMonthlyIncome());
        BigDecimal baseSurplus=income.subtract(currentExpense), scenarioSurplus=newIncome.subtract(destinationExpense);
        BigDecimal base=b.availableBalance(), scenario=b.availableBalance(); List<Map<String,Object>> months=new ArrayList<>(); YearMonth firstNegative=null; BigDecimal lowest=scenario;
        for(int i=0;i<req.horizonMonths();i++) { YearMonth month=start.plusMonths(i); boolean moved=!month.isBefore(req.moveMonth()); BigDecimal bs=baseSurplus, ss=moved?scenarioSurplus:baseSurplus;
            base=base.add(bs); scenario=scenario.add(ss); BigDecimal moving=BigDecimal.ZERO;
            if(month.equals(req.moveMonth())) { moving=req.movingCosts()==null?BigDecimal.ZERO:money(req.movingCosts()); scenario=scenario.subtract(moving); }
            base=money(base); scenario=money(scenario); lowest=lowest.min(scenario); if(firstNegative==null && scenario.signum()<0) firstNegative=month;
            months.add(Map.of("month",month.toString(),"baselineBalance",base,"scenarioBalance",scenario,"baselineSurplus",money(bs),"scenarioSurplus",money(ss),"movingCosts",money(moving)));
        }
        List<String> warnings=new ArrayList<>(); if(req.movingCosts()==null) warnings.add("Moving costs were excluded because no amount was provided; confirm zero or enter an estimate.");
        if(firstNegative!=null) warnings.add("Projected scenario balance first falls below zero in "+firstNegative+".");
        Map<String,String> methods=Map.of("rent",req.destinationRent()==null?"Rent Index ratio estimate":"User-provided destination rent override","groceries","Groceries Index ratio estimate","restaurants","Restaurant Price Index ratio estimate","otherLocationSensitive","Cost of Living Index rough fallback ratio estimate","fixed","Unchanged");
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("profileType","LOCAL_DEMO"); result.put("currency","USD"); result.put("originCity",b.currentCity()); result.put("destinationCity",to.city()); result.put("simulationStartMonth",start.toString()); result.put("moveMonth",req.moveMonth().toString()); result.put("horizonMonths",req.horizonMonths());
        result.put("currentExpenses",current); result.put("destinationExpenses",destination); result.put("baselineMonthlySurplus",money(baseSurplus)); result.put("scenarioMonthlySurplus",money(scenarioSurplus)); result.put("monthlySurplusDifference",money(scenarioSurplus.subtract(baseSurplus)));
        result.put("finalBaselineBalance",base); result.put("finalScenarioBalance",scenario); result.put("finalBalanceDifference",money(scenario.subtract(base))); result.put("upfrontMovingCosts",req.movingCosts()==null?null:money(req.movingCosts())); result.put("lowestProjectedScenarioBalance",lowest); result.put("firstNegativeMonth",firstNegative==null?"":firstNegative.toString()); result.put("months",months); result.put("methods",methods); result.put("datasetMetadata",cities.metadata());
        result.put("assumptions",List.of("Monthly buckets; move applies at the beginning of the selected month with no proration.","Household size, housing arrangement, and spending habits remain unchanged.","City indices are estimates and do not represent guaranteed personal costs.","Fixed monthly expenses remain unchanged.","Current balance as of "+b.balanceDate()+"; no inflation or investment growth modeled.")); result.put("warnings",warnings); return result;
    }
    private BigDecimal estimate(BigDecimal current,BigDecimal dest,BigDecimal origin) { if(origin.signum()<=0) throw new ScenarioException("INVALID_INDEX","City index must be positive."); return money(current.multiply(dest).divide(origin,8,RoundingMode.HALF_UP)); }
    private BigDecimal money(BigDecimal n) { return n.setScale(2,RoundingMode.HALF_UP); }
}

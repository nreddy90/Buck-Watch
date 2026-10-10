package com.buckwatch.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MoveScenarioRequest(String destinationCity, YearMonth moveMonth,
        Integer horizonMonths, BigDecimal movingCosts, BigDecimal newMonthlyIncome,
        BigDecimal destinationRent, String currency) { }

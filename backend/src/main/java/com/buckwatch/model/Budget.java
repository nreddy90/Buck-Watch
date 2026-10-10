package com.buckwatch.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Budget(String currentCity, String currency, BigDecimal monthlyIncome,
                     BigDecimal rent, BigDecimal groceries, BigDecimal restaurants,
                     BigDecimal otherLocationSensitive, BigDecimal fixedExpenses,
                     BigDecimal availableBalance, LocalDate balanceDate) { }

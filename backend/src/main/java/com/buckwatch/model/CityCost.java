package com.buckwatch.model;

import java.math.BigDecimal;

public record CityCost(String city, BigDecimal costOfLiving, BigDecimal rent,
                       BigDecimal groceries, BigDecimal restaurants) { }

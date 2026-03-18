package ru.neo.study.calculator.service.discounts.rules;

import ru.neo.study.calculator.service.discounts.parameters.DiscountsOptions;

import java.math.BigDecimal;

public interface DiscountRules {
    Boolean isApplicable(DiscountsOptions options);

    BigDecimal apply(BigDecimal rate);
}

package ru.neo.study.calculator.service.discounts.rules;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.neo.study.calculator.service.discounts.parameters.DiscountsOptions;
import ru.neo.study.calculator.service.discounts.parameters.DiscountsParam;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class SalaryClientRule implements DiscountRules {
    static final Logger logger = LoggerFactory.getLogger(SalaryClientRule.class);
    private final DiscountsParam discountsParam;

    public Boolean isApplicable(DiscountsOptions options) {
        return options.getIsSalaryClient();
    }

    public BigDecimal apply(BigDecimal rate) {
        logger.debug("Применена скидка зарплатного клиента {}, ставка {}", discountsParam.getSalaryClientDiscount(), rate);
        return rate.subtract(discountsParam.getSalaryClientDiscount());
    }
}

package ru.neo.study.calculator.service.discounts.rules;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.neo.study.calculator.service.discounts.parameters.DiscountsOptions;
import ru.neo.study.calculator.service.discounts.parameters.DiscountsParam;
import ru.neo.study.calculator.service.metrics.CalcService;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class InsuranceEnabledRule implements DiscountRules {
    static final Logger logger = LoggerFactory.getLogger(InsuranceEnabledRule.class);
    private final DiscountsParam discountsParam;

    public Boolean isApplicable(DiscountsOptions options) {
        return options.getIsInsuranceEnabled();
    }

    public BigDecimal apply(BigDecimal rate) {
        logger.debug("Применена скидка за страховку {} -> ставка {}", discountsParam.getInsuranceDiscount(), rate);
        return rate.subtract(discountsParam.getInsuranceDiscount());
    }
}

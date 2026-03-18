package ru.neo.study.calculator.service.discounts;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.neo.study.calculator.service.discounts.parameters.DiscountsOptions;
import ru.neo.study.calculator.service.discounts.rules.DiscountRules;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RulesProcessor {
    private final List<DiscountRules> rules;

    public BigDecimal applyAll(BigDecimal baseRate, DiscountsOptions options) {
        BigDecimal result = baseRate;

        for (DiscountRules rule : rules) {
            if (rule.isApplicable(options)) {
                result = rule.apply(result);
            }
        }

        return result;
    }
}

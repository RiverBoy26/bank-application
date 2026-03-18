package ru.neo.study.calculator.service.discounts.parameters;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "discount-properties")
@Data
public class DiscountsParam {
    private BigDecimal salaryClientDiscount;
    private BigDecimal insuranceDiscount;
}

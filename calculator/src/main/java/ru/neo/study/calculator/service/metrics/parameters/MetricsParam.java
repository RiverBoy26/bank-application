package ru.neo.study.calculator.service.metrics.parameters;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "credit-properties")
@Data
public class MetricsParam {
    private Integer loanRate;
    private BigDecimal insuranceRate;
    private BigDecimal largeAmountThreshold;
    private Integer longTermThreshold;
    private Integer scale;

    private BigDecimal minAmount;
    private Integer minTerm;

    private List<Integer> rangeAgeFemale;
    private List<Integer> rangeAgeMale;
    private Integer minTotalWorkMonth;
    private Integer minCurrentWorkMonth;
    private Integer maxCountSalary;
}

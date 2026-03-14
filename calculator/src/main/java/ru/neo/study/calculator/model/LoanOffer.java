package ru.neo.study.calculator.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class LoanOffer {
    private UUID statementId;
    private BigDecimal requestedAmount;
    private BigDecimal totalAmount;
    private Integer term;
    private BigDecimal monthlyPayment;
    private BigDecimal rate;
    private Boolean isInsuranceEnabled;
    private Boolean isSalaryClient;
}

package ru.neo.study.calculator.service.discounts.parameters;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DiscountsOptions {
    private Boolean isInsuranceEnabled;
    private Boolean isSalaryClient;
}

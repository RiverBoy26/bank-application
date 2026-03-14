package ru.neo.study.calculator.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Employment {
    private EmploymentStatus employmentStatus;
    private String employerINN;
    private BigDecimal salary;
    private Position position;
    private Integer workExperienceTotal;
    private Integer workExperienceCurrent;
}

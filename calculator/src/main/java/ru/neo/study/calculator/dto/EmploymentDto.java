package ru.neo.study.calculator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.neo.study.calculator.model.EmploymentStatus;
import ru.neo.study.calculator.model.Position;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class EmploymentDto {
    private EmploymentStatus employmentStatus;
    private String employerINN;
    private BigDecimal salary;
    private Position position;
    private Integer workExperienceTotal;
    private Integer workExperienceCurrent;
}

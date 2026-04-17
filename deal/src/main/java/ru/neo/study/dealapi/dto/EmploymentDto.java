package ru.neo.study.dealapi.dto;

import ru.neo.study.dealapi.enums.EmploymentStatus;
import ru.neo.study.dealapi.enums.Position;
import lombok.AllArgsConstructor;
import lombok.Data;

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

package ru.neo.study.calculator.model;

import lombok.Data;
import ru.neo.study.calculator.dto.EmploymentDto;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ScoringData {
    private BigDecimal amount;
    private Integer term;
    private String firstName;
    private String lastName;
    private String middleName;
    private Gender gender;
    private LocalDate birthdate;
    private String passportSeries;
    private String passportNumber;
    private LocalDate passportIssueDate;
    private String passportIssueBranch;
    private MaritalStatus maritalStatus;
    private Integer dependentAmount;
    private Employment employment;
    private String accountNumber;
    private Boolean isInsuranceEnabled;
    private Boolean isSalaryClient;
}

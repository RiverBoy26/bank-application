package dto;

import enums.Position;
import lombok.AllArgsConstructor;
import lombok.Data;
import enums.EmploymentStatus;

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

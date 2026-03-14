package ru.neo.study.calculator.mapper;

import lombok.experimental.UtilityClass;
import ru.neo.study.calculator.dto.EmploymentDto;
import ru.neo.study.calculator.model.Employment;

@UtilityClass
public class EmploymentMapper {
    public EmploymentDto toEmploymentDto(Employment employment) {
        return new EmploymentDto(
                employment.getEmploymentStatus(),
                employment.getEmployerINN(),
                employment.getSalary(),
                employment.getPosition(),
                employment.getWorkExperienceTotal(),
                employment.getWorkExperienceCurrent()
        );
    }

    public Employment toEmployment(EmploymentDto employmentDto) {
        Employment employment = new Employment();

        employment.setEmploymentStatus(employmentDto.getEmploymentStatus());
        employment.setEmployerINN(employmentDto.getEmployerINN());
        employment.setSalary(employmentDto.getSalary());
        employment.setPosition(employmentDto.getPosition());
        employment.setWorkExperienceTotal(employmentDto.getWorkExperienceTotal());
        employment.setWorkExperienceCurrent(employmentDto.getWorkExperienceCurrent());

        return employment;
    }
}

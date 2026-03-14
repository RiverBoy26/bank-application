package ru.neo.study.calculator.mapper;

import lombok.experimental.UtilityClass;
import ru.neo.study.calculator.dto.ScoringDataDto;
import ru.neo.study.calculator.model.ScoringData;

@UtilityClass
public class ScoringDataMapper {
    public ScoringDataDto toScoringDataDto(ScoringData scoringData) {

        return new ScoringDataDto(
                scoringData.getAmount(),
                scoringData.getTerm(),
                scoringData.getFirstName(),
                scoringData.getLastName(),
                scoringData.getMiddleName(),
                scoringData.getGender(),
                scoringData.getBirthdate(),
                scoringData.getPassportSeries(),
                scoringData.getPassportNumber(),
                scoringData.getPassportIssueDate(),
                scoringData.getPassportIssueBranch(),
                scoringData.getMaritalStatus(),
                scoringData.getDependentAmount(),
                EmploymentMapper.toEmploymentDto(scoringData.getEmployment()),
                scoringData.getAccountNumber(),
                scoringData.getIsInsuranceEnabled(),
                scoringData.getIsSalaryClient()
        );
    }

    public ScoringData toScoringData(ScoringDataDto scoringDataDto) {
        ScoringData scoringData = new ScoringData();

        scoringData.setAmount(scoringDataDto.getAmount());
        scoringData.setTerm(scoringDataDto.getTerm());
        scoringData.setFirstName(scoringDataDto.getFirstName());
        scoringData.setLastName(scoringDataDto.getLastName());
        scoringData.setMiddleName(scoringDataDto.getMiddleName());
        scoringData.setGender(scoringDataDto.getGender());
        scoringData.setPassportSeries(scoringDataDto.getPassportSeries());
        scoringData.setPassportNumber(scoringDataDto.getPassportNumber());
        scoringData.setPassportIssueDate(scoringDataDto.getPassportIssueDate());
        scoringData.setPassportIssueBranch(scoringDataDto.getPassportIssueBranch());
        scoringData.setMaritalStatus(scoringDataDto.getMaritalStatus());
        scoringData.setDependentAmount(scoringDataDto.getDependentAmount());
        scoringData.setEmployment(EmploymentMapper.toEmployment(scoringDataDto.getEmployment()));
        scoringData.setAccountNumber(scoringDataDto.getAccountNumber());
        scoringData.setIsInsuranceEnabled(scoringDataDto.getIsInsuranceEnabled());
        scoringData.setIsSalaryClient(scoringDataDto.getIsSalaryClient());

        return scoringData;
    }
}

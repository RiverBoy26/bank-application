package ru.neo.study.dealapi.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.neo.study.dealapi.dto.FinishRegistrationRequestDto;
import ru.neo.study.dealapi.dto.ScoringDataDto;
import ru.neo.study.dealapi.entity.Client;
import ru.neo.study.dealapi.entity.Statement;

@Mapper(componentModel = "spring")
public interface ScoringDataMapper {

    @Mapping(target = "amount", source = "statement.appliedOffer.requestedAmount")
    @Mapping(target = "term", source = "statement.appliedOffer.term")
    @Mapping(target = "firstName", source = "client.firstName")
    @Mapping(target = "lastName", source = "client.lastName")
    @Mapping(target = "middleName", source = "client.middleName")
    @Mapping(target = "gender", source = "request.gender")
    @Mapping(target = "birthdate", source = "client.birthDate")
    @Mapping(target = "passportSeries", source = "client.passport.series")
    @Mapping(target = "passportNumber", source = "client.passport.number")
    @Mapping(target = "passportIssueDate", source = "request.passportIssueDate")
    @Mapping(target = "passportIssueBranch", source = "request.passportIssueBranch")
    @Mapping(target = "maritalStatus", source = "request.maritalStatus")
    @Mapping(target = "dependentAmount", source = "request.dependentAmount")
    @Mapping(target = "employment", source = "request.employment")
    @Mapping(target = "accountNumber", source = "request.accountNumber")
    @Mapping(target = "isInsuranceEnabled", source = "statement.appliedOffer.isInsuranceEnabled")
    @Mapping(target = "isSalaryClient", source = "statement.appliedOffer.isSalaryClient")
    ScoringDataDto toDto(Client client, Statement statement, FinishRegistrationRequestDto request);
}
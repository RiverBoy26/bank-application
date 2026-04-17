package ru.neo.study.dealapi.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.neo.study.dealapi.dto.EmploymentDto;
import ru.neo.study.dealapi.dto.FinishRegistrationRequestDto;
import ru.neo.study.dealapi.dto.LoanStatementRequestDto;
import ru.neo.study.dealapi.entity.Client;
import ru.neo.study.dealapi.jsonb.Employment;
import ru.neo.study.dealapi.jsonb.Passport;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "birthDate", source = "birthdate")
    @Mapping(target = "passport", source = "dto")
    @Mapping(target = "employment", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "maritalStatus", ignore = true)
    @Mapping(target = "dependentAmount", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)
    Client toEntity(LoanStatementRequestDto dto);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "gender", source = "gender")
    @Mapping(target = "maritalStatus", source = "maritalStatus")
    @Mapping(target = "dependentAmount", source = "dependentAmount")
    @Mapping(target = "employment", source = "employment")
    @Mapping(target = "accountNumber", source = "accountNumber")
    void updateClientFromFinishRegistration(
            FinishRegistrationRequestDto dto,
            @MappingTarget Client client
    );

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "series", source = "passportSeries")
    @Mapping(target = "number", source = "passportNumber")
    @Mapping(target = "issueBranch", ignore = true)
    @Mapping(target = "issueDate", ignore = true)
    Passport toPassport(LoanStatementRequestDto dto);

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "status", source = "employmentStatus")
    @Mapping(target = "employerInn", source = "employerINN")
    Employment toEmployment(EmploymentDto dto);

    @AfterMapping
    default void updatePassportAfterFinishRegistration(
            FinishRegistrationRequestDto dto,
            @MappingTarget Client client
    ) {
        Passport passport = client.getPassport();
        if (passport == null) {
            passport = new Passport();
            passport.setId(UUID.randomUUID());
            client.setPassport(passport);
        } else if (passport.getId() == null) {
            passport.setId(UUID.randomUUID());
        }

        passport.setIssueDate(dto.getPassportIssueDate());
        passport.setIssueBranch(dto.getPassportIssueBranch());
    }

    @AfterMapping
    default void ensureEmploymentId(
            FinishRegistrationRequestDto dto,
            @MappingTarget Client client
    ) {
        if (client.getEmployment() != null && client.getEmployment().getId() == null) {
            client.getEmployment().setId(UUID.randomUUID());
        }
    }
}
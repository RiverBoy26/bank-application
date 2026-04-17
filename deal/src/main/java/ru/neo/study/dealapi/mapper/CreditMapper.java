package ru.neo.study.dealapi.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.neo.study.dealapi.dto.CreditDto;
import ru.neo.study.dealapi.entity.Credit;

@Mapper(componentModel = "spring")
public interface CreditMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "insuranceEnabled", source = "isInsuranceEnabled")
    @Mapping(target = "salaryClient", source = "isSalaryClient")
    @Mapping(target = "creditStatus", constant = "CALCULATED")
    Credit toEntity(CreditDto dto);
}
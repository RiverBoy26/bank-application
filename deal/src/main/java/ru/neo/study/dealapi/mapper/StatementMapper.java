package ru.neo.study.dealapi.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.neo.study.dealapi.dto.StatementStatusHistoryDto;
import ru.neo.study.dealapi.entity.Client;
import ru.neo.study.dealapi.entity.Statement;
import ru.neo.study.dealapi.enums.ApplicationStatus;
import ru.neo.study.dealapi.enums.ChangeType;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface StatementMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "client", source = "client")
    @Mapping(target = "credit", ignore = true)
    @Mapping(target = "status", source = "status")
    @Mapping(target = "creationDate", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "appliedOffer", ignore = true)
    @Mapping(target = "signDate", ignore = true)
    @Mapping(target = "sesCode", ignore = true)
    @Mapping(
            target = "statusHistory",
            expression = "java(new java.util.ArrayList<>(java.util.List.of(createInitialStatusHistory(status, changeType))))"
    )
    Statement toNewStatement(Client client, ApplicationStatus status, ChangeType changeType);

    default StatementStatusHistoryDto createInitialStatusHistory(
            ApplicationStatus status,
            ChangeType changeType
    ) {
        return new StatementStatusHistoryDto(
                status,
                java.time.LocalDateTime.now(),
                changeType
        );
    }

    default void appendStatusHistory(
            Statement statement,
            ApplicationStatus status,
            ChangeType changeType
    ) {
        List<StatementStatusHistoryDto> history = statement.getStatusHistory() == null
                ? new ArrayList<>()
                : new ArrayList<>(statement.getStatusHistory());

        history.add(createInitialStatusHistory(status, changeType));
        statement.setStatusHistory(history);
    }
}
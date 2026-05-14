package ru.neo.study.dealapi.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.neo.study.dealapi.dto.StatementStatusHistoryDto;
import ru.neo.study.dealapi.entity.Client;
import ru.neo.study.dealapi.entity.Statement;
import ru.neo.study.dealapi.enums.ApplicationStatus;
import ru.neo.study.dealapi.enums.ChangeType;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StatementMapperTest {

    private final StatementMapper mapper = Mappers.getMapper(StatementMapper.class);

    @Test
    void toNewStatementShouldCreateStatement() {
        Client client = mock(Client.class);

        Statement statement = mapper.toNewStatement(
                client,
                ApplicationStatus.PREAPPROVAL,
                ChangeType.AUTOMATIC
        );

        assertThat(statement.getClient()).isSameAs(client);
        assertThat(statement.getStatus()).isEqualTo(ApplicationStatus.PREAPPROVAL);
        assertThat(statement.getStatusHistory()).hasSize(1);
    }

    @Test
    void createInitialStatusHistoryShouldCreateHistoryElement() {
        StatementStatusHistoryDto history = mapper.createInitialStatusHistory(
                ApplicationStatus.APPROVED,
                ChangeType.MANUAL
        );

        assertThat(history.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(history.getChangeType()).isEqualTo(ChangeType.MANUAL);
        assertThat(history.getTime()).isNotNull();
    }

    @Test
    void appendStatusHistoryShouldAddHistoryElement() {
        Statement statement = Statement.builder()
                .statusHistory(List.of(
                        new StatementStatusHistoryDto(
                                ApplicationStatus.PREAPPROVAL,
                                LocalDateTime.now(),
                                ChangeType.AUTOMATIC
                        )
                ))
                .build();

        mapper.appendStatusHistory(
                statement,
                ApplicationStatus.APPROVED,
                ChangeType.MANUAL
        );

        assertThat(statement.getStatusHistory()).hasSize(2);
        assertThat(statement.getStatusHistory().get(1).getStatus())
                .isEqualTo(ApplicationStatus.APPROVED);
    }
}
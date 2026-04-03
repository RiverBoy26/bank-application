package ru.neo.study.dealapi.builders;

import dto.StatementStatusHistoryDto;
import enums.ApplicationStatus;
import enums.ChangeType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.neo.study.dealapi.entity.Statement;
import ru.neo.study.dealapi.repository.StatementRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class StatementAndStatusHistoryBuilder {
    private final StatementRepository statementRepository;

    public Statement getStatementById(UUID statementId) {
        return statementRepository.findById(statementId)
                .orElseThrow(() -> {
                    log.error("Заявка не найдена: statementId={}", statementId);
                    return new EntityNotFoundException("Заявка не найдена: " + statementId);
                });
    }

    public StatementStatusHistoryDto buildHistoryItem(ApplicationStatus status, ChangeType changeType) {
        return new StatementStatusHistoryDto(status, LocalDateTime.now(), changeType);
    }

    public void appendStatusHistory(Statement statement, ApplicationStatus status, ChangeType changeType) {
        List<StatementStatusHistoryDto> history = statement.getStatusHistory() == null
                ? new ArrayList<>()
                : new ArrayList<>(statement.getStatusHistory());
        history.add(buildHistoryItem(status, changeType));
        statement.setStatusHistory(history);
    }
}

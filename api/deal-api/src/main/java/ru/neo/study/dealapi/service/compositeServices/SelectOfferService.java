package ru.neo.study.dealapi.service.compositeServices;

import dto.LoanOfferDto;
import enums.ApplicationStatus;
import enums.ChangeType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.neo.study.dealapi.builders.StatementAndStatusHistoryBuilder;
import ru.neo.study.dealapi.entity.Statement;
import ru.neo.study.dealapi.repository.StatementRepository;

@Component
@Slf4j
@RequiredArgsConstructor
public class SelectOfferService {
    private final StatementAndStatusHistoryBuilder statementAndStatusHistoryBuilder;
    private final StatementRepository statementRepository;

    @Transactional
    public void selectOffer(LoanOfferDto loanOfferDto) {
        log.info("Получен запрос на выбор предложения: {}", loanOfferDto);

        if (loanOfferDto.getStatementId() == null) {
            log.error("В LoanOfferDto отсутствует statementId: {}", loanOfferDto);
            throw new IllegalArgumentException("В предложении отсутствует statementId");
        }

        Statement statement = statementAndStatusHistoryBuilder.getStatementById(loanOfferDto.getStatementId());
        log.debug("Найдена заявка для выбора предложения: statementId={}, currentStatus={}",
                statement.getId(), statement.getStatus());

        statement.setAppliedOffer(loanOfferDto);
        statement.setStatus(ApplicationStatus.APPROVED);
        statementAndStatusHistoryBuilder.appendStatusHistory(statement, ApplicationStatus.APPROVED, ChangeType.MANUAL);

        statementRepository.save(statement);
        log.info("Предложение выбрано и сохранено в заявке: statementId={}, newStatus={}",
                statement.getId(), statement.getStatus());
    }

}

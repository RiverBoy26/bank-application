package ru.neo.study.dealapi.service.compositeServices;

import dto.CreditDto;
import dto.FinishRegistrationRequestDto;
import dto.ScoringDataDto;
import enums.ApplicationStatus;
import enums.ChangeType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.neo.study.dealapi.builders.ClientBuilder;
import ru.neo.study.dealapi.builders.CreditBuilder;
import ru.neo.study.dealapi.builders.ScoringDataBuilder;
import ru.neo.study.dealapi.builders.StatementAndStatusHistoryBuilder;
import ru.neo.study.dealapi.calculatorClient.CalculatorClient;
import ru.neo.study.dealapi.entity.Client;
import ru.neo.study.dealapi.entity.Credit;
import ru.neo.study.dealapi.entity.Statement;
import ru.neo.study.dealapi.repository.ClientRepository;
import ru.neo.study.dealapi.repository.CreditRepository;
import ru.neo.study.dealapi.repository.StatementRepository;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class FinishRegistrationAndCalculateService {
    private final ClientBuilder clientBuilder;
    private final ClientRepository clientRepository;
    private final ScoringDataBuilder scoringDataBuilder;
    private final StatementAndStatusHistoryBuilder statementAndStatusHistoryBuilder;
    private final CalculatorClient calculatorClient;
    private final CreditRepository creditRepository;
    private final StatementRepository statementRepository;
    private final CreditBuilder creditBuilder;

    @Transactional
    public void finishRegistrationAndCalculate(UUID statementId, FinishRegistrationRequestDto finishRegistrationRequestDto) {
        log.info("Получен запрос на завершение регистрации и полный расчёт кредита: statementId={}, body={}",
                statementId, finishRegistrationRequestDto);

        Statement statement = statementAndStatusHistoryBuilder.getStatementById(statementId);
        Client client = statement.getClient();
        log.debug("Найдена заявка и клиент: statementId={}, clientId={}", statement.getId(), client.getId());

        clientBuilder.enrichClient(client, finishRegistrationRequestDto);
        clientRepository.save(client);
        log.info("Данные клиента обновлены для statementId={}: clientId={}", statement.getId(), client.getId());

        ScoringDataDto scoringDataDto = scoringDataBuilder.buildScoringData(statement, finishRegistrationRequestDto);
        log.debug("Сформирован ScoringDataDto для statementId={}: {}", statement.getId(), scoringDataDto);

        CreditDto creditDto = calculatorClient.calc(scoringDataDto);
        log.info("Получен результат полного расчёта кредита от calculator для statementId={}: {}",
                statement.getId(), creditDto);

        Credit credit = creditBuilder.buildCredit(creditDto);
        credit = creditRepository.save(credit);
        log.info("Сущность кредита сохранена: creditId={}, statementId={}", credit.getId(), statement.getId());

        statement.setCredit(credit);
        statement.setStatus(ApplicationStatus.CC_APPROVED);
        statementAndStatusHistoryBuilder.appendStatusHistory(statement, ApplicationStatus.CC_APPROVED, ChangeType.AUTOMATIC);

        statementRepository.save(statement);
        log.info("Заявка обновлена после полного расчёта кредита: statementId={}, status={}, creditId={}",
                statement.getId(), statement.getStatus(), credit.getId());
    }
}

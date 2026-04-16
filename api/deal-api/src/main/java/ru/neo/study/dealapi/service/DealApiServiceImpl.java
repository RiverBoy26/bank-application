package ru.neo.study.dealapi.service;

import jakarta.persistence.EntityNotFoundException;
import ru.neo.study.dealapi.mapper.ClientMapper;
import ru.neo.study.dealapi.mapper.CreditMapper;
import ru.neo.study.dealapi.mapper.ScoringDataMapper;
import ru.neo.study.dealapi.calculatorClient.CalculatorClient;
import ru.neo.study.dealapi.dto.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.neo.study.dealapi.entity.Client;
import ru.neo.study.dealapi.entity.Credit;
import ru.neo.study.dealapi.entity.Statement;
import ru.neo.study.dealapi.enums.ApplicationStatus;
import ru.neo.study.dealapi.enums.ChangeType;
import ru.neo.study.dealapi.mapper.StatementMapper;
import ru.neo.study.dealapi.repository.ClientRepository;
import ru.neo.study.dealapi.repository.CreditRepository;
import ru.neo.study.dealapi.repository.StatementRepository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DealApiServiceImpl implements DealApiService {
    private final ClientRepository clientRepository;
    private final StatementRepository statementRepository;
    private final CalculatorClient calculatorClient;
    private final ClientMapper clientMapper;
    private final StatementMapper statementMapper;
    private final ScoringDataMapper scoringDataMapper;
    private final CreditRepository creditRepository;
    private final CreditMapper creditMapper;

    private static final int LIST_OFFERS_SIZE = 4;


    @Override
    @Transactional
    public List<LoanOfferDto> calculateLoanTerms(LoanStatementRequestDto loanStatementRequestDto) {
        Client client = clientMapper.toEntity(loanStatementRequestDto);
        client = clientRepository.save(client);
        log.info("Клиент создан и сохранён: clientId={}", client.getId());

        Statement statement = statementMapper.toNewStatement(
                client,
                ApplicationStatus.PREAPPROVAL,
                ChangeType.AUTOMATIC
        );
        statement = statementRepository.save(statement);
        log.debug("Заявка создана и сохранена: statementId={}", statement.getId());

        List<LoanOfferDto> offers = calculatorClient.getOffers(loanStatementRequestDto);
        if (offers == null || offers.isEmpty()) {
            log.error("МС calculator вернул пустой список предложений для statementId={}", statement.getId());
            throw new IllegalStateException("Не удалось получить предложения по кредиту");
        }

        Statement finalStatement = statement;
        List<LoanOfferDto> normalizedOffers = offers.stream()
                .peek(offer -> offer.setStatementId(finalStatement.getId()))
                .sorted(Comparator.comparing(LoanOfferDto::getRate).reversed())
                .toList();

        if (normalizedOffers.size() != LIST_OFFERS_SIZE) {
            log.warn("Ожидалось 4 предложения, но для statementId={} получено {}", statement.getId(), normalizedOffers.size());
        }

        log.info("Для statementId={} получено {} предложений: {}",
                statement.getId(), normalizedOffers.size(), normalizedOffers);
        return normalizedOffers;
    }

    @Override
    @Transactional
    public void selectOffer(LoanOfferDto loanOfferDto) {
        log.info("Получен запрос на выбор предложения: {}", loanOfferDto);

        if (loanOfferDto.getStatementId() == null) {
            log.error("В LoanOfferDto отсутствует statementId: {}", loanOfferDto);
            throw new IllegalArgumentException("В предложении отсутствует statementId");
        }

        Statement statement = statementRepository.findById(loanOfferDto.getStatementId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Заявка с id " + loanOfferDto.getStatementId() + " не найдена"
                ));
        log.debug("Найдена заявка для выбора предложения: statementId={}, currentStatus={}",
                statement.getId(), statement.getStatus());

        statement.setAppliedOffer(loanOfferDto);
        statement.setStatus(ApplicationStatus.APPROVED);
        statementMapper.appendStatusHistory(statement, ApplicationStatus.APPROVED, ChangeType.MANUAL);

        statementRepository.save(statement);
        log.info("Предложение выбрано и сохранено в заявке: statementId={}, newStatus={}",
                statement.getId(), statement.getStatus());
    }

    @Override
    @Transactional
    public void finishRegistrationAndCalculate(UUID statementId, FinishRegistrationRequestDto finishRegistrationRequestDto) {

        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Заявка с id " + statementId + " не найдена"
                ));
        Client client = statement.getClient();
        log.debug("Найдена заявка и клиент: statementId={}, clientId={}", statement.getId(), client.getId());

        clientMapper.updateClientFromFinishRegistration(finishRegistrationRequestDto, client);
        clientRepository.save(client);
        log.info("Данные клиента обновлены для statementId={}: clientId={}", statement.getId(), client.getId());

        ScoringDataDto scoringDataDto = scoringDataMapper.toDto(client, statement, finishRegistrationRequestDto);
        log.debug("Сформирован ScoringDataDto для statementId={}: {}", statement.getId(), scoringDataDto);

        CreditDto creditDto = calculatorClient.calc(scoringDataDto);
        log.info("Получен результат полного расчёта кредита от calculator для statementId={}: {}",
                statement.getId(), creditDto);

        Credit credit = creditMapper.toEntity(creditDto);
        credit = creditRepository.save(credit);
        log.info("Сущность кредита сохранена: creditId={}, statementId={}", credit.getId(), statement.getId());

        statement.setCredit(credit);
        statement.setStatus(ApplicationStatus.CC_APPROVED);
        statementMapper.appendStatusHistory(statement, ApplicationStatus.CC_APPROVED, ChangeType.AUTOMATIC);

        statementRepository.save(statement);
        log.info("Заявка обновлена после полного расчёта кредита: statementId={}, status={}, creditId={}",
                statement.getId(), statement.getStatus(), credit.getId());
    }

    @Override
    public String getStatementStatus(UUID statementId) {
        return statementRepository.findById(statementId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Заявка с id " + statementId + " не найдена"
                ))
                .getStatus()
                .name();
    }
}

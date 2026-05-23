package ru.neo.study.dealapi.service;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import ru.neo.study.dealapi.enums.CreditStatus;
import ru.neo.study.dealapi.enums.Theme;
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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
    private final EmailMessageService emailMessageService;

    @Value("${ses-code-properties.list-offer-size}")
    private int LIST_OFFERS_SIZE;

    @Value("${ses-code-properties.min-ses-code}")
    private int MIN_SES_CODE;

    @Value("${ses-code-properties.max-ses-code-exclusive}")
    private int MAX_SES_CODE_EXCLUSIVE;

    @Override
    @Transactional
    public List<LoanOfferDto> calculateLoanTerms(LoanStatementRequestDto loanStatementRequestDto) {
        Client client = clientMapper.toEntity(loanStatementRequestDto);
        client = clientRepository.save(client);

        Statement statement = statementMapper.toNewStatement(
                client,
                ApplicationStatus.PREAPPROVAL,
                ChangeType.AUTOMATIC
        );
        statement = statementRepository.save(statement);

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

        log.debug("Для statementId={} получено {} предложений: {}",
                statement.getId(), normalizedOffers.size(), normalizedOffers);
        return normalizedOffers;
    }

    @Override
    @Transactional
    public void selectOffer(LoanOfferDto loanOfferDto) {

        if (loanOfferDto.getStatementId() == null) {
            log.error("В LoanOfferDto отсутствует statementId: {}", loanOfferDto);
            throw new IllegalArgumentException("В предложении отсутствует statementId");
        }

        Statement statement = statementRepository.findByIdWithBlock(loanOfferDto.getStatementId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Заявка с id " + loanOfferDto.getStatementId() + " не найдена"
                ));
        log.debug("Найдена заявка для выбора предложения: statementId={}, currentStatus={}",
                statement.getId(), statement.getStatus());

        statement.setAppliedOffer(loanOfferDto);
        statement.setStatus(ApplicationStatus.APPROVED);
        statementMapper.appendStatusHistory(statement, ApplicationStatus.APPROVED, ChangeType.MANUAL);

        statementRepository.save(statement);
        log.debug("Предложение выбрано и сохранено в заявке: statementId={}, newStatus={}",
                statement.getId(), statement.getStatus());

        emailMessageService.sendEmailMessage(
                statement,
                Theme.FINISH_REGISTRATION,
                "Кредитное предложение выбрано. Завершите регистрацию и заполните анкету."
        );
    }

    @Override
    @Transactional
    public void finishRegistrationAndCalculate(UUID statementId, FinishRegistrationRequestDto finishRegistrationRequestDto) {
        Statement statement = getStatementById(statementId);
        Client client = statement.getClient();
        log.debug("Найдена заявка и клиент: statementId={}, clientId={}", statement.getId(), client.getId());

        clientMapper.updateClientFromFinishRegistration(finishRegistrationRequestDto, client);
        clientRepository.save(client);
        log.debug("Данные клиента обновлены для statementId={}: clientId={}", statement.getId(), client.getId());

        ScoringDataDto scoringDataDto = scoringDataMapper.toDto(client, statement, finishRegistrationRequestDto);
        log.debug("Сформирован ScoringDataDto для statementId={}: {}", statement.getId(), scoringDataDto);

        CreditDto creditDto;
        try {
            creditDto = calculatorClient.calc(scoringDataDto);
        } catch (FeignException exception) {
            log.warn(
                    "При расчёте было отказано в кредите или возвращена ошибка: statementId={}, status={}, body={}",
                    statement.getId(),
                    exception.status(),
                    exception.contentUTF8()
            );

            statement.setStatus(ApplicationStatus.CC_DENIED);

            statementMapper.appendStatusHistory(
                    statement,
                    ApplicationStatus.CC_DENIED,
                    ChangeType.AUTOMATIC
            );

            statementRepository.save(statement);

            emailMessageService.sendEmailMessage(
                    statement,
                    Theme.STATEMENT_DENIED,
                    "По результатам скоринга заявка на кредит отклонена."
            );

            return;
        }

        log.debug("Получен результат полного расчёта кредита от calculator для statementId={}: {}",
                statement.getId(), creditDto);

        Credit credit = creditMapper.toEntity(creditDto);
        credit = creditRepository.save(credit);

        statement.setCredit(credit);
        statement.setStatus(ApplicationStatus.CC_APPROVED);
        statementMapper.appendStatusHistory(statement, ApplicationStatus.CC_APPROVED, ChangeType.AUTOMATIC);

        statementRepository.save(statement);
        log.debug("Заявка обновлена после полного расчёта кредита: statementId={}, status={}, creditId={}",
                statement.getId(), statement.getStatus(), credit.getId());

        emailMessageService.sendEmailMessage(
                statement,
                Theme.CREATE_DOCUMENTS,
                "Кредит одобрен. Документы по заявке будут сформированы."
        );
    }

    @Override
    public String getStatementStatus(UUID statementId) {
        return getStatementById(statementId)
                .getStatus()
                .name();
    }

    @Override
    @Transactional
    public void sendDocumentRequest(UUID statementId) {
        Statement statement = getStatementById(statementId);

        log.debug("Получен запрос на отправку документов: statementId={}, currentStatus={}",
                statement.getId(), statement.getStatus());

        statement.setStatus(ApplicationStatus.DOCUMENT_CREATED);

        statementMapper.appendStatusHistory(
                statement, ApplicationStatus.DOCUMENT_CREATED, ChangeType.AUTOMATIC);

        log.debug(
                "Статус заявки изменён и добавлена запись о формировании документов: statementId={}, status={}",
                statement.getId(), statement.getStatus());

        statementRepository.save(statement);

        emailMessageService.sendEmailMessage(
                statement, Theme.SEND_DOCUMENTS, "Документы по кредиту сформированы и отправлены клиенту.");
    }

    @Override
    @Transactional
    public void signDocumentRequest(UUID statementId) {
        Statement statement = getStatementById(statementId);

        int sesCode = ThreadLocalRandom.current().nextInt(
                MIN_SES_CODE, MAX_SES_CODE_EXCLUSIVE);

        statement.setSesCode(sesCode);
        statementRepository.save(statement);

        log.debug(
                "SES-код сгенерирован и сохранён в заявке: statementId={}, status={}",
                statement.getId(), statement.getStatus());

        emailMessageService.sendEmailMessage(
                statement, Theme.SEND_SES, "Код подписания документов: " + sesCode);
    }

    @Override
    @Transactional
    public void signDocument(UUID statementId, Integer sesCode) {
        Statement statement = getStatementById(statementId);

        if (statement.getSesCode() == null || !statement.getSesCode().equals(sesCode)) {
            log.warn("Некорректный SES-код: statementId={}", statement.getId());
            throw new IllegalArgumentException("Некорректный SES-код");
        }

        statement.setStatus(ApplicationStatus.DOCUMENT_SIGNED);
        statement.setSignDate(LocalDateTime.now());

        statementMapper.appendStatusHistory(
                statement,
                ApplicationStatus.DOCUMENT_SIGNED,
                ChangeType.MANUAL
        );

        statementRepository.save(statement);

        log.debug(
                "Документы подписаны: statementId={}, status={}, signDate={}",
                statement.getId(),
                statement.getStatus(),
                statement.getSignDate()
        );
    }

    @Override
    @Transactional
    public void issueCredit(UUID statementId) {
        Statement statement = getStatementById(statementId);

        if (statement.getStatus() != ApplicationStatus.DOCUMENT_SIGNED) {
            throw new IllegalStateException("Кредит можно выдать только после подписания документов");
        }

        if (statement.getCredit() != null) {
            statement.getCredit().setCreditStatus(CreditStatus.ISSUED);
        }

        statement.setStatus(ApplicationStatus.CREDIT_ISSUED);

        statementMapper.appendStatusHistory(
                statement,
                ApplicationStatus.CREDIT_ISSUED,
                ChangeType.AUTOMATIC
        );

        statementRepository.save(statement);

        emailMessageService.sendEmailMessage(
                statement,
                Theme.CREDIT_ISSUED,
                "Документы подписаны. Кредит успешно выдан."
        );
    }

    private Statement getStatementById(UUID statementId) {
        return statementRepository.findById(statementId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Заявка с id " + statementId + " не найдена"
                ));
    }
}

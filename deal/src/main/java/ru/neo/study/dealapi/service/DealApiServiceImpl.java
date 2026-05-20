package ru.neo.study.dealapi.service;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
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

    private static final int LIST_OFFERS_SIZE = 4;
    private static final int MIN_SES_CODE = 1000;
    private static final int MAX_SES_CODE_EXCLUSIVE = 10000;

    public static final String FINISH_REGISTRATION_TEXT = "Кредитное предложение выбрано. Завершите регистрацию и заполните анкету.";

    public static final String CREATE_DOCUMENTS_TEXT = "Кредит одобрен. Документы по заявке будут сформированы.";

    public static final String SEND_DOCUMENTS_TEXT = "Документы по кредиту сформированы и отправлены клиенту.";

    public static final String SEND_SES_TEXT = "Код подписания документов: ";

    public static final String CREDIT_ISSUED_TEXT = "Документы подписаны. Кредит успешно выдан.";

    public static final String STATEMENT_DENIED_TEXT = "По результатам скоринга заявка на кредит отклонена.";


    @Override
    @Transactional
    public List<LoanOfferDto> calculateLoanTerms(LoanStatementRequestDto loanStatementRequestDto) {
        Client client = clientMapper.toEntity(loanStatementRequestDto);
        client = clientRepository.save(client);
        log.debug("Клиент создан и сохранён: clientId={}", client.getId());

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
                FINISH_REGISTRATION_TEXT
        );
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
                    STATEMENT_DENIED_TEXT
            );

            return;
        }

        log.debug("Получен результат полного расчёта кредита от calculator для statementId={}: {}",
                statement.getId(), creditDto);

        Credit credit = creditMapper.toEntity(creditDto);
        credit = creditRepository.save(credit);
        log.debug("Сущность кредита сохранена: creditId={}, statementId={}", credit.getId(), statement.getId());

        statement.setCredit(credit);
        statement.setStatus(ApplicationStatus.CC_APPROVED);
        statementMapper.appendStatusHistory(statement, ApplicationStatus.CC_APPROVED, ChangeType.AUTOMATIC);

        statementRepository.save(statement);
        log.debug("Заявка обновлена после полного расчёта кредита: statementId={}, status={}, creditId={}",
                statement.getId(), statement.getStatus(), credit.getId());

        emailMessageService.sendEmailMessage(
                statement,
                Theme.CREATE_DOCUMENTS,
                CREATE_DOCUMENTS_TEXT
        );
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

    @Override
    @Transactional
    public void sendDocumentRequest(UUID statementId) {
        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Заявка с id " + statementId + " не найдена"
                ));

        log.debug(
                "Получен запрос на отправку документов: statementId={}, currentStatus={}",
                statement.getId(),
                statement.getStatus()
        );

        statement.setStatus(ApplicationStatus.DOCUMENT_CREATED);

        log.debug(
                "Статус заявки изменён при отправке документов: statementId={}, status={}",
                statement.getId(),
                ApplicationStatus.DOCUMENT_CREATED
        );

        statementMapper.appendStatusHistory(
                statement,
                ApplicationStatus.DOCUMENT_CREATED,
                ChangeType.AUTOMATIC
        );

        log.debug(
                "В историю статусов добавлена запись о формировании документов: statementId={}, status={}",
                statement.getId(),
                statement.getStatus()
        );

        statementRepository.save(statement);

        log.debug(
                "Заявка сохранена после подготовки документов к отправке: statementId={}, status={}",
                statement.getId(),
                statement.getStatus()
        );

        emailMessageService.sendEmailMessage(
                statement,
                Theme.SEND_DOCUMENTS,
                SEND_DOCUMENTS_TEXT
        );
    }

    @Override
    @Transactional
    public void signDocumentRequest(UUID statementId) {
        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Заявка с id " + statementId + " не найдена"
                ));

        log.debug(
                "Заявка найдена для подписания документов: statementId={}, currentStatus={}, clientId={}",
                statement.getId(),
                statement.getStatus(),
                statement.getClient() != null ? statement.getClient().getId() : null
        );

        int sesCode = ThreadLocalRandom.current().nextInt(
                MIN_SES_CODE,
                MAX_SES_CODE_EXCLUSIVE
        );

        log.debug(
                "SES-код сгенерирован для заявки: statementId={}, sesCode={}",
                statement.getId(),
                sesCode
        );

        statement.setSesCode(sesCode);
        statementRepository.save(statement);

        log.debug(
                "SES-код сохранён в заявке: statementId={}, status={}",
                statement.getId(),
                statement.getStatus()
        );

        emailMessageService.sendEmailMessage(
                statement,
                Theme.SEND_SES,
                SEND_SES_TEXT + sesCode
        );
    }

    @Override
    @Transactional
    public void signDocument(UUID statementId, Integer sesCode) {
        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Заявка с id " + statementId + " не найдена"
                ));

        log.debug(
                "Заявка найдена для проверки SES-кода: statementId={}, currentStatus={}, storedSesCodeExists={}",
                statement.getId(),
                statement.getStatus(),
                statement.getSesCode() != null
        );

        if (statement.getSesCode() == null || !statement.getSesCode().equals(sesCode)) {
            log.warn(
                    "Некорректный SES-код: statementId={}, expected={}, actual={}",
                    statement.getId(),
                    statement.getSesCode(),
                    sesCode
            );

            throw new IllegalArgumentException("Некорректный SES-код");
        }

        log.debug(
                "SES-код успешно подтверждён: statementId={}, currentStatus={}",
                statement.getId(),
                statement.getStatus()
        );

        statement.setStatus(ApplicationStatus.DOCUMENT_SIGNED);
        statement.setSignDate(LocalDateTime.now());

        log.debug(
                "Документы подписаны: statementId={}, status={}, signDate={}",
                statement.getId(),
                statement.getStatus(),
                statement.getSignDate()
        );

        statementMapper.appendStatusHistory(
                statement,
                ApplicationStatus.DOCUMENT_SIGNED,
                ChangeType.MANUAL
        );

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
                CREDIT_ISSUED_TEXT
        );
    }
}

package ru.neo.study.dealapi.service.compositeServices;

import ru.neo.study.dealapi.dto.LoanOfferDto;
import ru.neo.study.dealapi.dto.LoanStatementRequestDto;
import ru.neo.study.dealapi.enums.ApplicationStatus;
import ru.neo.study.dealapi.enums.ChangeType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.neo.study.dealapi.builders.ClientBuilder;
import ru.neo.study.dealapi.builders.StatementAndStatusHistoryBuilder;
import ru.neo.study.dealapi.calculatorClient.CalculatorClient;
import ru.neo.study.dealapi.entity.Client;
import ru.neo.study.dealapi.entity.Statement;
import ru.neo.study.dealapi.repository.ClientRepository;
import ru.neo.study.dealapi.repository.StatementRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CalculateLoanTermsService {
    private final ClientRepository clientRepository;
    private final StatementRepository statementRepository;
    private final CalculatorClient calculatorClient;
    private final ClientBuilder clientBuilder;
    private final StatementAndStatusHistoryBuilder statementAndStatusHistoryBuilder;

    @Transactional
    public List<LoanOfferDto> calculateLoanTerms(LoanStatementRequestDto loanStatementRequestDto) {

        log.info("Получен запрос на предварительный расчёт кредита: {}", loanStatementRequestDto);

        Client client = clientBuilder.buildClient(loanStatementRequestDto);
        client = clientRepository.save(client);
        log.info("Клиент создан и сохранён: clientId={}", client.getId());

        Statement statement = Statement.builder()
                .client(client)
                .status(ApplicationStatus.PREAPPROVAL)
                .creationDate(LocalDateTime.now())
                .statusHistory(new ArrayList<>(List.of(statementAndStatusHistoryBuilder.buildHistoryItem(
                        ApplicationStatus.PREAPPROVAL, ChangeType.AUTOMATIC))))
                .build();
        statement = statementRepository.save(statement);
        log.info("Заявка создана и сохранена: statementId={}", statement.getId());

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

        if (normalizedOffers.size() != 4) {
            log.warn("Ожидалось 4 предложения, но для statementId={} получено {}", statement.getId(), normalizedOffers.size());
        }

        log.info("Для statementId={} получено {} предложений: {}",
                statement.getId(), normalizedOffers.size(), normalizedOffers);
        return normalizedOffers;
    }
}

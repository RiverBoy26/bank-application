package ru.neo.study.statement.service;

import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.neo.study.statement.dealClient.DealClient;
import ru.neo.study.statement.dto.LoanOfferDto;
import ru.neo.study.statement.dto.LoanStatementRequestDto;
import ru.neo.study.statement.exception.OffersNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementServiceImpl implements StatementService {
    private final DealClient dealClient;

    private static final int MAX_ATTEMPTS = 3;

    public List<LoanOfferDto> calculateLoanOffers(LoanStatementRequestDto loanStatementRequestDto) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                log.debug("Отправка запроса в микросервис deal: {}", loanStatementRequestDto);
                ResponseEntity<List<LoanOfferDto>> response = dealClient.calculateLoanTerms(loanStatementRequestDto);
                if (response != null && response.getBody() != null && !response.getBody().isEmpty()) {
                    List<LoanOfferDto> offers = response.getBody();
                    log.debug("Полученные предложения от микросервиса deal: {}", offers);
                    return offers;
                }
            } catch (RetryableException | FeignException.FeignServerException ex) {
                log.warn("Попытка {} из {} завершилась ошибкой при расчёте кредитных предложений",
                        attempt, MAX_ATTEMPTS, ex);
            }
        }

        throw new OffersNotFoundException("Кредитные предложения не найдены");
    }

    public void selectOffer(LoanOfferDto loanOfferDto) {
        log.debug("Отправка выбранного предложения в микросервис deal: {}", loanOfferDto);
        dealClient.selectOffer(loanOfferDto);

        log.debug("Выбранное кредитное предложение успешно отправлено в микросервис deal. ID заявки: {}",
                loanOfferDto.getStatementId());
    }
}

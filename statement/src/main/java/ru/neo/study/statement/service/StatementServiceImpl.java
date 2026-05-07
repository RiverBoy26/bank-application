package ru.neo.study.statement.service;

import feign.FeignException;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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

    @Retry(name = "dealClientRetry")
    @CircuitBreaker(name = "dealClientCircuitBreaker", fallbackMethod = "calculateLoanOffersFallback")
    public List<LoanOfferDto> calculateLoanOffers(LoanStatementRequestDto loanStatementRequestDto) {
        log.debug("Отправка запроса в микросервис deal: {}", loanStatementRequestDto);

        ResponseEntity<List<LoanOfferDto>> response = dealClient.calculateLoanTerms(loanStatementRequestDto);
        List<LoanOfferDto> offers = response.getBody();

        if (offers == null || offers.isEmpty()) {
            throw new OffersNotFoundException("Кредитные предложения не найдены");
        }

        log.debug("Полученные предложения от микросервиса deal: {}", offers);
        return offers;
    }

    public void selectOffer(LoanOfferDto loanOfferDto) {
        log.debug("Отправка выбранного предложения в микросервис deal: {}", loanOfferDto);
        dealClient.selectOffer(loanOfferDto);

        log.debug("Выбранное кредитное предложение успешно отправлено в микросервис deal. ID заявки: {}",
                loanOfferDto.getStatementId());
    }

    private List<LoanOfferDto> calculateLoanOffersFallback(
            LoanStatementRequestDto request,
            Throwable ex
    ) {
        log.warn("Fallback метода calculateLoanOffers. Причина: {}", ex.getMessage());
        throw new OffersNotFoundException(
                "Кредитные предложения не найдены. Повторите попытку позже."
        );
    }
}

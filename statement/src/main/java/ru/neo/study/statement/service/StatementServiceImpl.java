package ru.neo.study.statement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.neo.study.statement.dealClient.DealClient;
import ru.neo.study.statement.dto.LoanOfferDto;
import ru.neo.study.statement.dto.LoanStatementRequestDto;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementServiceImpl implements StatementService {
    private final DealClient dealClient;

    public List<LoanOfferDto> calculateLoanOffers(LoanStatementRequestDto loanStatementRequestDto) {
        log.debug("Отправка запроса в микросервис deal: {}", loanStatementRequestDto);
        List<LoanOfferDto> offers = dealClient.calculateLoanTerms(loanStatementRequestDto).getBody();

        log.debug("Полученные предложения от микросервиса deal: {}", offers);
        return offers;
    }

    public void selectOffer(LoanOfferDto loanOfferDto) {
        log.debug("Отправка выбранного предложения в микросервис deal: {}", loanOfferDto);
        dealClient.selectOffer(loanOfferDto);

        log.debug("Выбранное кредитное предложение успешно отправлено в микросервис deal. ID заявки: {}",
                loanOfferDto.getStatementId());
    }
}

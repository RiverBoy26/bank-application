package ru.neo.study.dealapi.calculatorClient;

import dto.CreditDto;
import dto.LoanOfferDto;
import dto.LoanStatementRequestDto;
import dto.ScoringDataDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CalculatorClient {

    private final RestClient calculatorRestClient;

    public List<LoanOfferDto> getOffers(LoanStatementRequestDto request) {
        return calculatorRestClient.post()
                .uri("/calculator/offers")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<List<LoanOfferDto>>() {});
    }

    public CreditDto calc(ScoringDataDto request) {
        return calculatorRestClient.post()
                .uri("/calculator/calc")
                .body(request)
                .retrieve()
                .body(CreditDto.class);
    }
}

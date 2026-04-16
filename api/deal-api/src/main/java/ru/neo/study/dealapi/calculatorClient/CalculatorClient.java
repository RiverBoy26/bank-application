package ru.neo.study.dealapi.calculatorClient;

import ru.neo.study.dealapi.dto.CreditDto;
import ru.neo.study.dealapi.dto.LoanOfferDto;
import ru.neo.study.dealapi.dto.LoanStatementRequestDto;
import ru.neo.study.dealapi.dto.ScoringDataDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "calculator-api",
        url = "${calculator-api.base-url}",
        path = "/calculator"
)
public interface CalculatorClient {

    @PostMapping("/offers")
    List<LoanOfferDto> getOffers(@RequestBody LoanStatementRequestDto request);

    @PostMapping("/calc")
    CreditDto calc(@RequestBody ScoringDataDto request);
}

package ru.neo.study.statement.dealClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.neo.study.statement.dto.LoanOfferDto;
import ru.neo.study.statement.dto.LoanStatementRequestDto;

import java.util.List;

@FeignClient(
        name = "deal-api",
        url = "${deal-api.base-url}",
        path = "/deal"
)
public interface DealClient {
    @PostMapping("/statement")
    ResponseEntity<List<LoanOfferDto>> calculateLoanTerms(@RequestBody LoanStatementRequestDto loanStatementRequestDto);

    @PostMapping("/offer/select")
    ResponseEntity<Void> selectOffer(@RequestBody LoanOfferDto loanOfferDto);
}

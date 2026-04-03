package ru.neo.study.dealapi.controller;

import dto.FinishRegistrationRequestDto;
import dto.LoanOfferDto;
import dto.LoanStatementRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.neo.study.dealapi.service.DealApiService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/deal")
public class DealApiController {
    private final DealApiService dealApiService;

    @PostMapping("/statement")
    public ResponseEntity<List<LoanOfferDto>> calculateLoanTerms(@RequestBody LoanStatementRequestDto loanStatementRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dealApiService.calculateLoanTerms(loanStatementRequestDto));
    }

    @PostMapping("/offer/select")
    @ResponseStatus(HttpStatus.CREATED)
    public void selectOffer(@RequestBody LoanOfferDto loanOfferDto) {
        dealApiService.selectOffer(loanOfferDto);
    }

    @PostMapping("/calculate/{statementId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void finishRegistrationAndCalculate(@PathVariable UUID statementId,
                                               @RequestBody FinishRegistrationRequestDto finishRegistrationRequestDto) {
        dealApiService.finishRegistrationAndCalculate(statementId, finishRegistrationRequestDto);
    }
}

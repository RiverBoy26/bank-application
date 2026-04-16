package ru.neo.study.dealapi.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.neo.study.dealapi.dto.FinishRegistrationRequestDto;
import ru.neo.study.dealapi.dto.LoanOfferDto;
import ru.neo.study.dealapi.dto.LoanStatementRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.neo.study.dealapi.service.DealApiService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/deal")
public class DealApiController {
    private final DealApiService dealApiService;

    @PostMapping("/statement")
    public ResponseEntity<List<LoanOfferDto>> calculateLoanTerms(@RequestBody LoanStatementRequestDto loanStatementRequestDto) {
        log.info("Получен запрос на предварительный расчёт кредита: {}", loanStatementRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dealApiService.calculateLoanTerms(loanStatementRequestDto));
    }

    @PostMapping("/offer/select")
    public ResponseEntity<Void> selectOffer(@RequestBody LoanOfferDto loanOfferDto) {
        dealApiService.selectOffer(loanOfferDto);
        return ResponseEntity
                .ok()
                .header("Location", "/statement/" + loanOfferDto.getStatementId())
                .build();
    }

    @PostMapping("/calculate/{statementId}")
    public ResponseEntity<Void> finishRegistrationAndCalculate(@PathVariable UUID statementId,
                                                                   @RequestBody FinishRegistrationRequestDto finishRegistrationRequestDto) {
        log.info("Получен запрос на завершение регистрации и полный расчёт кредита: statementId={}, body={}",
                statementId, finishRegistrationRequestDto);
        dealApiService.finishRegistrationAndCalculate(statementId, finishRegistrationRequestDto);
        return ResponseEntity
                .ok()
                .header("StatementStatus", dealApiService.getStatementStatus(statementId))
                .build();
    }
}

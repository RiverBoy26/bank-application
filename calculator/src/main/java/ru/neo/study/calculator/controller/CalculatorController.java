package ru.neo.study.calculator.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.neo.study.calculator.dto.CreditDto;
import ru.neo.study.calculator.dto.LoanOfferDto;
import ru.neo.study.calculator.dto.LoanStatementRequestDto;
import ru.neo.study.calculator.dto.ScoringDataDto;
import ru.neo.study.calculator.service.CalculatorService;

import java.util.List;

@RestController
@RequestMapping("/calculator")
@AllArgsConstructor
public class CalculatorController {
    private CalculatorService calculatorService;

    @PostMapping("/offers")
    public List<LoanOfferDto> offers(@RequestBody LoanStatementRequestDto loanStatementRequest) {
        return calculatorService.offers(loanStatementRequest);
    }

    @PostMapping("/calc")
    public CreditDto calc(@RequestBody ScoringDataDto scoringData) {
        return calculatorService.calc(scoringData);
    }
}

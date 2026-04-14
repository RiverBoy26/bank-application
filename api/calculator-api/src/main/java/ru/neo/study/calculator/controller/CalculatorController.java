package ru.neo.study.calculator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    @Operation(
            summary = "Получить 4 кредитных предложения",
            description = "Выполняет прескоринг и возвращает 4 объекта LoanOfferDto"
    )
    @ApiResponse(responseCode = "200", description = "Предложения рассчитаны")
    public List<LoanOfferDto> offers(@RequestBody LoanStatementRequestDto loanStatementRequest) {
        return calculatorService.offers(loanStatementRequest);
    }

    @PostMapping("/calc")
    @Operation(
            summary = "Выполнить полный расчет кредита",
            description = "Выполняет скоринг и возвращает объект CreditDto"
    )
    @ApiResponse(responseCode = "200", description = "Кредит рассчитан")
    public CreditDto calc(@RequestBody ScoringDataDto scoringData) {
        return calculatorService.calc(scoringData);
    }
}

package ru.neo.study.calculator.service;

import ru.neo.study.calculator.dto.CreditDto;
import ru.neo.study.calculator.dto.LoanOfferDto;
import ru.neo.study.calculator.dto.LoanStatementRequestDto;
import ru.neo.study.calculator.dto.ScoringDataDto;

import java.util.List;

public interface CalculatorService {
    List<LoanOfferDto> offers(LoanStatementRequestDto loanStatementRequest);

    CreditDto calc(ScoringDataDto scoringData);
}

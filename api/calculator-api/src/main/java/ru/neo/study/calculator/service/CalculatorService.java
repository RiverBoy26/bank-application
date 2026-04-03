package ru.neo.study.calculator.service;

import dto.CreditDto;
import dto.LoanOfferDto;
import dto.LoanStatementRequestDto;
import dto.ScoringDataDto;

import java.util.List;

public interface CalculatorService {
    List<LoanOfferDto> offers(LoanStatementRequestDto loanStatementRequest);

    CreditDto calc(ScoringDataDto scoringData);
}

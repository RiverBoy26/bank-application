package ru.neo.study.calculator.repository;

import ru.neo.study.calculator.dto.CreditDto;
import ru.neo.study.calculator.dto.LoanOfferDto;
import ru.neo.study.calculator.dto.LoanStatementRequestDto;
import ru.neo.study.calculator.model.LoanStatementRequest;
import ru.neo.study.calculator.model.ScoringData;

import java.math.BigDecimal;
import java.util.List;

public interface CalculatorStorage {
    List<LoanOfferDto> offers(LoanStatementRequest loanStatementRequest);

    CreditDto calc(ScoringData scoringData, BigDecimal rate);
}

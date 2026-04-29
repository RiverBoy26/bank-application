package ru.neo.study.statement.service;

import ru.neo.study.statement.dto.LoanOfferDto;
import ru.neo.study.statement.dto.LoanStatementRequestDto;

import java.util.List;

public interface StatementService {
    List<LoanOfferDto> calculateLoanOffers(LoanStatementRequestDto loanStatementRequestDto);

    void selectOffer(LoanOfferDto loanOfferDto);
}

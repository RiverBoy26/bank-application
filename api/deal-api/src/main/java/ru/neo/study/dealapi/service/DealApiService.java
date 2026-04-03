package ru.neo.study.dealapi.service;

import dto.FinishRegistrationRequestDto;
import dto.LoanOfferDto;
import dto.LoanStatementRequestDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

public interface DealApiService {
    List<LoanOfferDto> calculateLoanTerms(LoanStatementRequestDto loanStatementRequestDto);

    void selectOffer(LoanOfferDto loanOfferDto);

    void finishRegistrationAndCalculate(UUID statementId, FinishRegistrationRequestDto finishRegistrationRequestDto);
}

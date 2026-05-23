package ru.neo.study.dealapi.service;

import ru.neo.study.dealapi.dto.FinishRegistrationRequestDto;
import ru.neo.study.dealapi.dto.LoanOfferDto;
import ru.neo.study.dealapi.dto.LoanStatementRequestDto;
import ru.neo.study.dealapi.entity.Statement;

import java.util.List;
import java.util.UUID;

public interface DealApiService {
    List<LoanOfferDto> calculateLoanTerms(LoanStatementRequestDto loanStatementRequestDto);

    void selectOffer(LoanOfferDto loanOfferDto);

    void finishRegistrationAndCalculate(UUID statementId, FinishRegistrationRequestDto finishRegistrationRequestDto);

    String getStatementStatus(UUID statementId);

    void sendDocumentRequest(UUID statementId);

    void signDocumentRequest(UUID statementId);

    void signDocument(UUID statementId, Integer sesCode);

    void issueCredit(UUID statementId);
}

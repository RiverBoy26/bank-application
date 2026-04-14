package ru.neo.study.dealapi.service;

import ru.neo.study.dealapi.dto.FinishRegistrationRequestDto;
import ru.neo.study.dealapi.dto.LoanOfferDto;
import ru.neo.study.dealapi.dto.LoanStatementRequestDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.neo.study.dealapi.service.compositeServices.CalculateLoanTermsService;
import ru.neo.study.dealapi.service.compositeServices.FinishRegistrationAndCalculateService;
import ru.neo.study.dealapi.service.compositeServices.SelectOfferService;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DealApiServiceImpl implements DealApiService {
    private final CalculateLoanTermsService calculateLoanTermsService;
    private final SelectOfferService selectOfferService;
    private final FinishRegistrationAndCalculateService finishRegistrationAndCalculateService;

    @Override
    @Transactional
    public List<LoanOfferDto> calculateLoanTerms(LoanStatementRequestDto loanStatementRequestDto) {
        return calculateLoanTermsService.calculateLoanTerms(loanStatementRequestDto);
    }

    @Override
    public void selectOffer(LoanOfferDto loanOfferDto) {
        selectOfferService.selectOffer(loanOfferDto);
    }

    @Override
    public void finishRegistrationAndCalculate(UUID statementId, FinishRegistrationRequestDto finishRegistrationRequestDto) {
        finishRegistrationAndCalculateService.finishRegistrationAndCalculate(statementId, finishRegistrationRequestDto);
    }
}
